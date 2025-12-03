package com.know_who_how.main_server.inheritance;

import com.know_who_how.main_server.global.entity.Inheritance.Inheritance;
import com.know_who_how.main_server.global.entity.Inheritance.InheritanceRecipient;
import com.know_who_how.main_server.global.entity.Inheritance.InheritanceVideo;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.inheritance.service.EmailService;
import com.know_who_how.main_server.inheritance.dto.RecipientRegistrationRequest;
import com.know_who_how.main_server.inheritance.repository.InheritanceRecipientRepository;
import com.know_who_how.main_server.inheritance.repository.InheritanceRepository;
import com.know_who_how.main_server.inheritance.repository.InheritanceVideoRepository;
import com.know_who_how.main_server.inheritance.service.InheritanceService;
import com.know_who_how.main_server.inheritance.service.S3Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
class AccessControlServiceTest {

    @InjectMocks
    private InheritanceService inheritanceService;

    // 모든 의존성 Mocking
    @Mock private InheritanceVideoRepository videoRepository;
    @Mock private InheritanceRecipientRepository recipientRepository;
    @Mock private S3Service s3Service;
    @Mock private EmailService emailService;
    @Mock private InheritanceRepository inheritanceRepository;

    // 테스트용 상수 및 Mock 객체
    private static final Long TEST_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long TEST_VIDEO_ID = 50L;
    private static final String TEST_EMAIL = "test@example.com";
    private static final LocalDateTime SCHEDULED_DATE = LocalDateTime.now().plusDays(1);

    private Inheritance mockInheritance;
    private InheritanceVideo mockVideo;
    private User mockOwner;
    private static final String MOCK_DOWNLOAD_URL = "http://mockurl.com/download";

    @BeforeEach
    void setUp() {
        // --- Arrange ---
        // 공통 Mock 객체 생성
        mockOwner = mock(User.class);
        given(mockOwner.getUserId()).willReturn(TEST_USER_ID);

        mockInheritance = mock(Inheritance.class);
        given(mockInheritance.getUser()).willReturn(mockOwner);

        mockVideo = mock(InheritanceVideo.class);
        given(mockVideo.getInheritance()).willReturn(mockInheritance);
        given(mockVideo.getS3ObjectKey()).willReturn("/path/to/video.mp4");

        given(videoRepository.findById(TEST_VIDEO_ID)).willReturn(Optional.of(mockVideo));

        // [S3 Mock] 다운로드 URL 반환 Mocking 설정
        given(s3Service.generateDownloadPresignedUrl(anyString())).willReturn(MOCK_DOWNLOAD_URL);

        // save() 호출 시 실제 토큰이 생성되도록 Mocking
        given(recipientRepository.save(any(InheritanceRecipient.class)))
                .willAnswer(invocation -> {
                    // JPA ID 할당은 생략하고, 토큰이 생성되었다는 것만 확인
                    return invocation.getArgument(0);
                });
    }

    /**
     * IH-ACCESS-01: 수신자 등록 (토큰 생성)
     */
    @Test
    @DisplayName("3-1. [성공] 수신자 등록: Recipient 레코드 생성 및 토큰 생성 확인")
    void registerRecipients_success() {
        // Arrange
        List<RecipientRegistrationRequest> request = List.of(
                new RecipientRegistrationRequest(TEST_EMAIL, SCHEDULED_DATE)
        );

        // Act
        inheritanceService.registerRecipients(TEST_USER_ID, TEST_VIDEO_ID, request);

        // Assert
        // 1. recipientRepository.save() 호출 대신, Video 객체의 addRecipient 메서드 호출 확인 (Cascade 의존)
        then(mockVideo).should().addRecipient(any(InheritanceRecipient.class));

        // 2. save가 불필요하게 호출되지 않았는지 확인
        then(recipientRepository).should(never()).save(any(InheritanceRecipient.class));
    }

    /**
     * IH-ACCESS-02: 수신자 등록 (소유권 없음)
     */
    @Test
    @DisplayName("3-2. [실패] 수신자 등록: 다른 사용자가 접근 시 FORBIDDEN 예외 발생")
    void registerRecipients_failure_forbiddenAccess() {
        // Arrange
        List<RecipientRegistrationRequest> request = List.of(
                new RecipientRegistrationRequest(TEST_EMAIL, SCHEDULED_DATE)
        );

        // Act & Assert
        // 다른 사용자 ID (OTHER_USER_ID)로 접근 시도
        assertThatThrownBy(() -> inheritanceService.registerRecipients(OTHER_USER_ID, TEST_VIDEO_ID, request))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_INHERITANCE_ACCESS);

        // 실패했으므로, save 메서드가 호출되지 않았는지 확인
        then(recipientRepository).should(never()).save(any(InheritanceRecipient.class));
    }

    /**
     * IH-ACCESS-03: 토큰 유효성 검증 및 열람 성공
     */
    @Test
    @DisplayName("3-3. [성공] 토큰 유효성 검증: Presigned URL 발급 및 토큰 사용 처리")
    void validateToken_success() {
        // Arrange
        String validToken = UUID.randomUUID().toString();

        // 1. Mock Recipient 객체 생성 및 isLinkUsed=false (미사용 상태) 설정
        InheritanceRecipient mockRecipient = mock(InheritanceRecipient.class);
        given(mockRecipient.isLinkUsed()).willReturn(false); // 미사용 상태
        given(mockRecipient.getVideo()).willReturn(mockVideo); // 영상 연결

        // 2. findByAccessLink가 성공적으로 Recipient를 반환하도록 Mocking
        given(recipientRepository.findByAccessLink(validToken)).willReturn(Optional.of(mockRecipient));

        // Act
        String presignedUrl = inheritanceService.getPresignedUrlAndValidateToken(validToken);

        // Assert
        // 1. S3Service의 다운로드 URL 생성 메서드가 호출되었는지 확인
        then(s3Service).should().generateDownloadPresignedUrl(mockVideo.getS3ObjectKey());

        // 2. 반환된 URL이 Mocking된 URL과 일치하는지 확인
        assertEquals(MOCK_DOWNLOAD_URL, presignedUrl);

        // 3. 토큰이 사용되었음을 표시하는 메서드(markLinkUsed)가 호출되었는지 확인 (보안 검증)
        then(mockRecipient).should().markLinkUsed();
    }

    /**
     * IH-ACCESS-04: 토큰 사용 후 재사용 시도
     */
    @Test
    @DisplayName("3-4. [실패] 사용된 토큰 재사용 시도 시 INVALID_ACCESS_TOKEN 예외 발생")
    void validateToken_failure_alreadyUsed() {
        // Arrange
        String usedToken = UUID.randomUUID().toString();
        // 1. Mock Recipient 객체 생성 및 isLinkUsed=true (사용 완료 상태) 설정
        InheritanceRecipient mockRecipient = mock(InheritanceRecipient.class);
        given(mockRecipient.isLinkUsed()).willReturn(true);
        given(mockRecipient.getVideo()).willReturn(mockVideo);

        // 2. findByAccessLink가 Recipient를 반환하도록 Mocking
        given(recipientRepository.findByAccessLink(usedToken)).willReturn(Optional.of(mockRecipient));

        // Act & Assert
        assertThatThrownBy(() -> inheritanceService.getPresignedUrlAndValidateToken(usedToken))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ACCESS_TOKEN);

        // 실패했으므로, S3 URL 발급 메서드가 호출되지 않았는지 확인
        then(s3Service).should(never()).generateDownloadPresignedUrl(anyString());

        // 토큰 무효화 메서드가 재호출되지 않았는지 확인
        then(mockRecipient).should(never()).markLinkUsed();
    }
}