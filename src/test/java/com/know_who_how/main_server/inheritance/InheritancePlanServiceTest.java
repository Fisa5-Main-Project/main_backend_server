package com.know_who_how.main_server.inheritance;

import com.know_who_how.main_server.global.entity.Inheritance.Inheritance;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.inheritance.dto.InheritancePlanResponse;
import com.know_who_how.main_server.inheritance.repository.InheritanceVideoRepository;
import com.know_who_how.main_server.inheritance.service.EmailService;
import com.know_who_how.main_server.inheritance.service.InheritanceService;
import com.know_who_how.main_server.inheritance.service.S3Service;
import com.know_who_how.main_server.inheritance.repository.InheritanceRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoMoreInteractions;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class InheritancePlanServiceTest {

    @InjectMocks
    private InheritanceService inheritanceService;

    // 모든 의존성은 Mocking
    @Mock private UserRepository userRepository;
    @Mock private InheritanceRepository inheritanceRepository;
    @Mock private InheritanceVideoRepository videoRepository;
    @Mock private S3Service s3Service;
    @Mock private EmailService emailService;

    // 테스트용 상수 및 Mock 객체
    private static final Long TEST_USER_ID = 1L;
    private static final BigDecimal ASSET = new BigDecimal("1000000000.00");
    private static final String RATIO = "A:50,B:50";

    private User mockUser;
    private Inheritance mockInheritance;

    @BeforeEach
    void setUp() {
        // [Arrange] 공통 Mock 객체 생성
        mockUser = mock(User.class);

        // 각 테스트에서 필요한 시점에 isRegistered의 상태 재정의

        mockInheritance = mock(Inheritance.class);
        given(mockInheritance.getInheritanceId()).willReturn(1L);
        given(mockInheritance.getAsset()).willReturn(ASSET);
        given(mockInheritance.getRatio()).willReturn(RATIO);

        // 기본 설정: 모든 save/findById 호출에 대해 Mocking 설정
        given(userRepository.findById(any())).willReturn(Optional.of(mockUser));
        given(inheritanceRepository.save(any(Inheritance.class))).willReturn(mockInheritance);
    }

    /**
     * IH-PLAN-01 (생성) 및 IH-PLAN-03 (조회 성공) 검증
     */
    @Test
    @DisplayName("1-1. 최초 상속 계획 등록 성공 및 User 상태 True 검증")
    void registerNewInheritancePlan_success() {
        // Arrange
        // 최초 생성 상황: User는 미등록 상태라고 가정
        given(mockUser.isUserInheritanceRegistration()).willReturn(false);
        given(inheritanceRepository.findByUser_UserId(TEST_USER_ID)).willReturn(Optional.empty());

        // Act
        Long inheritanceId = inheritanceService.saveOrUpdateInheritancePlan(TEST_USER_ID, ASSET, RATIO);

        // Assert
        assertNotNull(inheritanceId);

        // 1. User 레코드의 상태 변경 메서드가 호출되었는지 확인 (false -> true)
        then(mockUser).should().markInheritanceRegistered();

        // 2. Inheritance 레코드가 DB에 저장 요청되었는지 확인
        then(inheritanceRepository).should().save(any(Inheritance.class));

        // 3. (IH-PLAN-03) 조회 시 데이터가 올바르게 매핑되어 반환되는지 확인
        given(inheritanceRepository.findByUser_UserId(TEST_USER_ID)).willReturn(Optional.of(mockInheritance));
        InheritancePlanResponse response = inheritanceService.getInheritancePlan(TEST_USER_ID);
        assertEquals(ASSET, response.asset());
        assertEquals(RATIO, response.ratio());
    }

    /**
     * IH-PLAN-02 (업데이트) 검증
     */
    @Test
    @DisplayName("1-2. 기존 상속 계획 데이터 업데이트 및 ID 불변 검증")
    void updateExistingInheritancePlan_success() {
        // Arrange
        BigDecimal UPDATED_ASSET = new BigDecimal("2000000000.00");
        String UPDATED_RATIO = "A:80,B:20";

        // User는 이미 등록된 상태라고 가정 (markInheritanceRegistered() 호출 방지)
        given(mockUser.isUserInheritanceRegistration()).willReturn(true);

        // 기존 Inheritance 객체가 발견된다고 가정 (업데이트 상황)
        given(inheritanceRepository.findByUser_UserId(TEST_USER_ID)).willReturn(Optional.of(mockInheritance));

        // Act
        Long resultId = inheritanceService.saveOrUpdateInheritancePlan(TEST_USER_ID, UPDATED_ASSET, UPDATED_RATIO);

        // Assert
        // 1. 반환 ID가 기존 ID와 동일한지 확인 (ID 불변)
        assertEquals(mockInheritance.getInheritanceId(), resultId);

        // 2. Mock Inheritance 객체의 업데이트 메서드가 호출되었는지 확인
        then(mockInheritance).should().updatePlan(UPDATED_ASSET, UPDATED_RATIO);

        // 3. User의 상태 변경 메서드(markInheritanceRegistered)가 호출되지 않았는지 확인
        then(mockUser).should(never()).markInheritanceRegistered();
    }

    /**
     * IH-PLAN-04 (조회 실패) 검증
     */
    @Test
    @DisplayName("1-3. 상속 계획 미등록 시 조회 실패 (INHERITANCE_NOT_FOUND) 예외 발생 검증")
    void getInheritancePlan_failure_whenPlanNotFound() {
        // Arrange
        // findByUser_UserId 호출 시 Optional.empty() 반환 (상속 계획 없음)
        given(inheritanceRepository.findByUser_UserId(TEST_USER_ID)).willReturn(Optional.empty());

        // Act & Assert
        // getInheritancePlan 호출 시 INHERITANCE_NOT_FOUND 예외가 발생하는지 확인
        assertThatThrownBy(() -> inheritanceService.getInheritancePlan(TEST_USER_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INHERITANCE_NOT_FOUND);

        // Mock 객체에 대한 불필요한 상호작용 검증
        then(inheritanceRepository).should().findByUser_UserId(TEST_USER_ID);
        verifyNoMoreInteractions(inheritanceRepository);
        verifyNoMoreInteractions(userRepository); // UserRepository는 findById로 user를 찾았으므로 제외
    }
}