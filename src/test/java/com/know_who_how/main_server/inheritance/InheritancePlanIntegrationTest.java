package com.know_who_how.main_server.inheritance;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.entity.User.Gender;
import com.know_who_how.main_server.global.entity.User.InvestmentTendancy;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.inheritance.dto.InheritancePlanResponse;
import com.know_who_how.main_server.inheritance.service.InheritanceService;
import com.know_who_how.main_server.inheritance.service.S3Service;
import com.know_who_how.main_server.inheritance.service.EmailService;
import com.know_who_how.main_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.assertj.core.api.Assertions.assertThatThrownBy; // 예외 검증용

@ActiveProfiles("test")
@SpringBootTest
@Transactional
class InheritancePlanIntegrationTest {

    // 테스트 대상 Service (실제 로직 사용)
    @Autowired
    private InheritanceService inheritanceService;

    // 실제 DB 접근 Repository (테스트에선 H2 DB에 데이터 넣기 위함)
    @Autowired
    private UserRepository userRepository;

    // 외부 통신은 Mockito로 대체
    @MockBean
    private S3Service s3Service;
    @MockBean
    private EmailService emailService;

    private Long testUserId;

    // 테스트용 자산 값
    private static final BigDecimal INITIAL_ASSET = new BigDecimal("1000000000.00");
    private static final String INITIAL_RATIO = "spouse:50,child1:20,child2:30";
    private static final BigDecimal UPDATED_ASSET = new BigDecimal("2000000000.00");
    private static final String UPDATED_RATIO = "spouse:55,child:25,child:20";


    @BeforeEach
    void setUp() {
        // --- Arrange(준비) ---
        // 테스트 사용자 생성 및 DB 저장
        User user = User.builder()
                .loginId("testuser_loginid")
                .password("encodedPassword123")
                .phoneNum("01012345678")
                .birth(LocalDate.of(1990, 1, 1))
                .gender(Gender.M)
                .name("테스터")
                .assetTotal(1000000L)
                .investmentTendancy(InvestmentTendancy.공격투자형)
                .build();

        userRepository.save(user);
        testUserId = user.getUserId();
    }

    // ----------------------------------------------------

    /**
     * [1] 최초 상속 계획 등록 및 User 상태 업데이트
     */
    @Test
    @DisplayName("1-1. 최초 상속 계획 등록 성공 및 User 상태 True 검증")
    void registerNewInheritancePlan() {
        // --- Act(실행) ---
        // 상속 계획 등록 실행
        Long inheritanceId = inheritanceService.saveOrUpdateInheritancePlan(testUserId, INITIAL_ASSET, INITIAL_RATIO);

        // --- Assert(검증) ---

        // 계획이 성공적으로 생성되었는지 확인
        assertNotNull(inheritanceId, "Inheritance ID는 null이 아니어야 합니다.");

        // User 상태가 업데이트되었는지 확인
        User updatedUser = userRepository.findById(testUserId).orElseThrow();
        assertTrue(updatedUser.isUserInheritanceRegistration(), "상속 등록 상태가 TRUE로 업데이트되어야 합니다.");

        // 조회 시 데이터가 올바른지 확인
        InheritancePlanResponse response = inheritanceService.getInheritancePlan(testUserId);
        assertEquals(inheritanceId, response.inheritanceId());
        assertEquals(INITIAL_ASSET.stripTrailingZeros(), response.asset().stripTrailingZeros(), "저장된 자산 금액이 일치해야 합니다.");
        assertEquals(INITIAL_RATIO, response.ratio(), "저장된 비율이 일치해야 합니다.");
    }

    /**
     * [2]기존 상속 계획 업데이트
     */
    @Test
    @DisplayName("1-2. 기존 상속 계획 데이터 업데이트 검증")
    void updateExistingInheritancePlan() {
        // --- Arrange(준비) ---
        // 계획 등록하여 기존 레코드 생성
        Long firstId = inheritanceService.saveOrUpdateInheritancePlan(testUserId, INITIAL_ASSET, INITIAL_RATIO);

        // --- Act(실행) ---
        // 새로운 데이터로 업데이트 실행
        Long secondId = inheritanceService.saveOrUpdateInheritancePlan(testUserId, UPDATED_ASSET, UPDATED_RATIO);

        // --- Assert(검증) ---

        // ID가 새로 생성되지 않고 기존 ID와 동일한지 확인
        assertEquals(firstId, secondId, "업데이트 시 ID가 변경되면 안 됩니다.");

        // DB에서 조회 시 업데이트된 값으로 변경되었는지 확인
        InheritancePlanResponse response = inheritanceService.getInheritancePlan(testUserId);
        assertEquals(UPDATED_ASSET.stripTrailingZeros(), response.asset().stripTrailingZeros());
        assertEquals(UPDATED_RATIO, response.ratio());

        // User의 상속 등록 상태는 여전히 true인지 확인
        User finalUser = userRepository.findById(testUserId).orElseThrow();
        assertTrue(finalUser.isUserInheritanceRegistration());
    }

    /**
     * [3] 상속 계획 조회 실패
     */
    @Test
    @DisplayName("1-3. 상속 계획 미등록 시 조회 실패 예외 발생 검증")
    void getInheritancePlan_shouldThrowException_whenPlanNotFound() {
        // Act(실행) & Assert(검증)
        // 상속 계획을 등록하지 않은 상태(지금 test에서는 상속 X상태임)에서 조회할 때 getInheritancePlan 호출 시 INHERITANCE_NOT_FOUND 예외가 발생하는지 확인
        assertThatThrownBy(() -> inheritanceService.getInheritancePlan(testUserId))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INHERITANCE_NOT_FOUND);
    }
}