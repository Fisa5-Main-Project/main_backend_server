package com.know_who_how.main_server.inheritance;

import com.know_who_how.main_server.global.entity.Inheritance.Inheritance;
import com.know_who_how.main_server.global.entity.Inheritance.InheritanceVideo;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.inheritance.dto.VideoUploadCompleteRequest;
import com.know_who_how.main_server.inheritance.dto.VideoUploadInitResponse;
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

import java.net.MalformedURLException;
import java.util.Optional;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.isNull;

@MockitoSettings(strictness = Strictness.LENIENT)
@ExtendWith(MockitoExtension.class)
public class VideoManagementServiceTest {

    @InjectMocks
    private InheritanceService inheritanceService;

    @Mock private InheritanceRepository inheritanceRepository;
    @Mock private InheritanceVideoRepository videoRepository;
    @Mock private S3Service s3Service;

    private static final Long TEST_USER_ID = 1L;
    private static final Long OTHER_USER_ID = 2L;
    private static final Long TEST_INHERITANCE_ID = 10L;
    private static final Long MOCK_VIDEO_ID = 99L;
    private static final String MOCK_UPLOAD_ID = "mock-upload-id-xyz";

    private User mockOwner;
    private Inheritance mockInheritance;
    private InheritanceVideo mockVideo;

    @BeforeEach
    void setup() throws MalformedURLException {
        // --- Arrange ---

        mockOwner = mock(User.class);
        given(mockOwner.getUserId()).willReturn(TEST_USER_ID);

        mockInheritance = mock(Inheritance.class);
        given(mockInheritance.getInheritanceId()).willReturn(TEST_INHERITANCE_ID);
        given(mockInheritance.getUser()).willReturn(mockOwner);

        // S3Service의 Presigned URL 반환 값 Mocking
        given(s3Service.generatePartPresignedUrl(anyString(), eq(MOCK_UPLOAD_ID), anyInt()))
                .willReturn("http://mockurl.com/part_1");
        given(s3Service.initiateMultipartUpload(anyString()))
                .willReturn(MOCK_UPLOAD_ID);
        given(s3Service.generateDownloadPresignedUrl(anyString()))
                .willReturn("http://mockurl.com/download");

        // save() 호출 시 ID를 강제 할당하여 videoId가 null이 되지 않도록 설정
        given(videoRepository.save(any(InheritanceVideo.class)))
                .willAnswer(invocation -> {
                    InheritanceVideo video = invocation.getArgument(0);
                    video.setVideoId(MOCK_VIDEO_ID); // @Setter를 통해 ID 할당
                    return video;
                });

        // 영상 삭제/완료 테스트를 위해 사용할 MockVideo 객체
        mockVideo = mock(InheritanceVideo.class);
        given(mockVideo.getS3ObjectKey()).willReturn("/path/to/test-video-key.mp4");
    }

    /**
     * IH-VIDEO-01: 영상 업로드 초기화 성공
     */
    @Test
    @DisplayName("2-1. [성공] 영상 업로드 초기화: InheritanceVideo 생성 및 S3 Upload ID 반환")
    void initiateVideoUpload_success() {
        // Arrange
        given(inheritanceRepository.findById(TEST_INHERITANCE_ID)).willReturn(Optional.of(mockInheritance));
        given(mockInheritance.getVideo()).willReturn(null); // 영상이 아직 없다고 Mocking

        // Act
        VideoUploadInitResponse response = inheritanceService.initiateVideoUpload(TEST_USER_ID, TEST_INHERITANCE_ID);

        // Assert
        assertEquals(MOCK_VIDEO_ID, response.videoId());
        assertEquals(MOCK_UPLOAD_ID, response.uploadId());

        // InheritanceVideo 엔티티가 DB에 저장(영속화) 요청되었는지 확인
        then(videoRepository).should().save(any(InheritanceVideo.class));

        // S3Service의 초기화 메서드가 호출되었는지 확인
        then(s3Service).should().initiateMultipartUpload(anyString());
    }

    /**
     * IH-VIDEO-02: 영상 초기화 (소유권 없음)
     */
    @Test
    @DisplayName("2-2. [실패] 영상 초기화: 소유권이 다르면 FORBIDDEN 예외 발생")
    void initiateVideoUpload_failure_forbiddenAccess() {
        // Arrange
        given(inheritanceRepository.findById(TEST_INHERITANCE_ID)).willReturn(Optional.of(mockInheritance));

        // Act & Assert (OTHER_USER_ID로 접근 시도)
        assertThatThrownBy(() -> inheritanceService.initiateVideoUpload(OTHER_USER_ID, TEST_INHERITANCE_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN_INHERITANCE_ACCESS);
    }

    /**
     * IH-VIDEO-03: 영상 초기화 (중복 등록)
     */
    @Test
    @DisplayName("2-3. [실패] 영상 초기화: 기존 영상이 있으면 VIDEO_ALREADY_EXISTS 예외 발생")
    void initiateVideoUpload_failure_videoAlreadyExists() {
        // Arrange
        given(inheritanceRepository.findById(TEST_INHERITANCE_ID)).willReturn(Optional.of(mockInheritance));
        given(mockInheritance.getVideo()).willReturn(mockVideo); // 이미 영상이 존재한다고 Mocking

        // Act & Assert
        assertThatThrownBy(() -> inheritanceService.initiateVideoUpload(TEST_USER_ID, TEST_INHERITANCE_ID))
                .isInstanceOf(CustomException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.VIDEO_ALREADY_EXISTS);
    }

    /**
     * IH-VIDEO-04: MultiPart 업로드 완료 처리 (Complete)
     */
    @Test
    @DisplayName("2-4. [성공] MultiPart 업로드 완료: S3Service.completeMultipartUpload 호출 확인")
    void completeVideoUpload_success() {
        // Arrange
        // Repository가 Inheritance 객체를 반환하도록 설정
        given(inheritanceRepository.findById(TEST_INHERITANCE_ID)).willReturn(Optional.of(mockInheritance));
        given(mockInheritance.getVideo()).willReturn(mockVideo);

        // 완료 요청 DTO 준비 (가짜 ETag 리스트 포함)
        VideoUploadCompleteRequest request = new VideoUploadCompleteRequest(
                MOCK_UPLOAD_ID,
                // List.of 대신 java.util.List.of()를 사용하고 DTO 객체 생성
                java.util.List.of(new com.know_who_how.main_server.inheritance.dto.PartETagDTO(1, "ETAG1"))
        );

        // Act
        inheritanceService.completeVideoUpload(TEST_USER_ID, TEST_INHERITANCE_ID, request);

        // Assert
        // 1. S3Service의 완료 메서드가 정확한 파라미터로 호출되었는지 확인 (가장 중요)
        then(s3Service).should().completeMultipartUpload(
                eq(mockVideo.getS3ObjectKey()), // S3 Object Key
                eq(MOCK_UPLOAD_ID), // Upload ID
                any(List.class) // PartETag 리스트
        );
    }

    /**
     * IH-VIDEO-05: 영상 편지 삭제 성공
     */
    @Test
    @DisplayName("2-5. [성공] 영상 편지 삭제: S3 파일 및 DB 레코드 제거 확인")
    void deleteVideo_success() {
        // Arrange
        given(inheritanceRepository.findById(TEST_INHERITANCE_ID)).willReturn(Optional.of(mockInheritance));
        given(mockInheritance.getVideo()).willReturn(mockVideo); // 영상이 존재한다고 Mocking

        // Act
        inheritanceService.deleteVideo(TEST_USER_ID, TEST_INHERITANCE_ID);

        // Assert
        // 1. S3 파일 삭제가 호출되었는지 확인
        then(s3Service).should().deleteObject(eq(mockVideo.getS3ObjectKey()));

        // 2. InheritanceVideo DB 레코드 삭제가 호출되었는지 확인
        then(videoRepository).should().delete(eq(mockVideo));

        // 3. Inheritance 엔티티의 연관 관계가 끊어졌는지 확인
        then(mockInheritance).should().setVideo(isNull());
    }
}