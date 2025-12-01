package com.know_who_how.main_server.inheritance.service;

import com.amazonaws.services.s3.model.PartETag;
import com.know_who_how.main_server.global.entity.Inheritance.Inheritance;
import com.know_who_how.main_server.global.entity.Inheritance.InheritanceRecipient;
import com.know_who_how.main_server.global.entity.Inheritance.InheritanceVideo;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.inheritance.dto.*;
import com.know_who_how.main_server.inheritance.repository.InheritanceRecipientRepository;
import com.know_who_how.main_server.inheritance.repository.InheritanceRepository;
import com.know_who_how.main_server.inheritance.repository.InheritanceVideoRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InheritanceService {

    private final UserRepository userRepository;
    private final InheritanceRepository inheritanceRepository;
    private final InheritanceVideoRepository videoRepository;
    private final InheritanceRecipientRepository recipientRepository;
    private final S3Service s3Service;

    // --- 상속 조회 및 계획 관리 ---


    /**
     * user가 상속 설계를 받은 적이 있는지 여부 반환
     * @param userId 사용자 ID
     * @return true/false
     */
    @Transactional(readOnly = true)
    public InheritanceStatusResponse getInheritanceRegistrationStatus(Long userId){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        return new InheritanceStatusResponse(user.isUserInheritanceRegistration());
    }

    @Transactional
    public Long saveOrUpdateInheritancePlan(Long userId, BigDecimal asset, String ratio){
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Inheritance inheritance = inheritanceRepository.findByUser_UserId(userId)
                .orElseGet(() -> Inheritance.builder()
                        .user(user)
                        .asset(asset)
                        .ratio(ratio)
                        .build());

        if(inheritance.getInheritanceId() != null){
            inheritance.updatePlan(asset, ratio);
        }

        Inheritance savedInheritance = inheritanceRepository.save(inheritance);

        // 상속 계획 등록 완료 후 User 상태 업데이트
        if (!user.isUserInheritanceRegistration()) {
            user.markInheritanceRegistered();
        }

        return savedInheritance.getInheritanceId();
    }

    /**
     * [1] Multipart Upload 시작 및 S3 Upload ID 반환
     */
    @Transactional
    public VideoUploadInitResponse initiateVideoUpload(Long userId, Long inheritanceId){

        Inheritance inheritance = inheritanceRepository.findById(inheritanceId)
                .orElseThrow(() -> new CustomException(ErrorCode.INHERITANCE_NOT_FOUND));

        if(!inheritance.getUser().getUserId().equals(userId)){
            throw new CustomException(ErrorCode.FORBIDDEN_INHERITANCE_ACCESS);
        }

        // 이미 영상이 존재하면 에러
        if(inheritance.getVideo() !=null){
            throw new CustomException(ErrorCode.VIDEO_ALREADY_EXISTS);
        }

        // S3 객체 키 생성
        String videoKey = String.format("inheritance/user_%d/video_%s.mp4",
                userId, UUID.randomUUID().toString());

        // InheritanceVideo 엔티티 생성 (업로드 시작 기록)
        InheritanceVideo video = InheritanceVideo.builder()
                .inheritance(inheritance)
                .s3ObjectKey(videoKey)
                .build();
        videoRepository.save(video);

        // S3 Multipart Upload 시작 및 Upload ID 획득
        String uploadId = s3Service.initiateMultipartUpload(videoKey);

        return new VideoUploadInitResponse(
                inheritanceId,
                video.getVideoId(),
                uploadId,
                videoKey
        );
    }

    /**
     * [2] 조각(Part) 업로드용 Presigned URL 발급
     */
    @Transactional(readOnly = true)
    public VideoPartUrlResponse generatePartUploadUrl(Long userId, Long inheritanceId, String uploadId, int partNumber){

        // S3 객체 키를 찾기 위해 Inheritance 객체 조회
        Inheritance inheritance = inheritanceRepository.findById(inheritanceId)
                .orElseThrow(() -> new CustomException(ErrorCode.INHERITANCE_NOT_FOUND));

        if(!inheritance.getUser().getUserId().equals(userId)){
            throw new CustomException(ErrorCode.FORBIDDEN_INHERITANCE_ACCESS);
        }

        // InheritanceVideo 객체가 존재하는지 확인 (초기화 단계 건너뛰기 방지)
        InheritanceVideo video = inheritance.getVideo();
        if (video == null) {
            // InheritanceVideo가 없으면 초기화(initiate)가 수행되지 않은 것
            throw new CustomException(ErrorCode.VIDEO_NOT_FOUND);
        }

        // S3Service를 통해 Part Upload용 Presigned URL 발급
        String s3ObjectKey = inheritance.getVideo().getS3ObjectKey();
        String partUploadUrl = s3Service.generatePartPresignedUrl(s3ObjectKey, uploadId, partNumber);

        return new VideoPartUrlResponse(
                partNumber,
                partUploadUrl
        );
    }

    /**
     * [3] Multipart Upload 완료
     */
    @Transactional
    public void completeVideoUpload(Long userId, Long inheritanceId, VideoUploadCompleteRequest request){

        Inheritance inheritance = inheritanceRepository.findById(inheritanceId)
                .orElseThrow(() -> new CustomException(ErrorCode.INHERITANCE_NOT_FOUND));

        if(!inheritance.getUser().getUserId().equals(userId)){
            throw new CustomException(ErrorCode.FORBIDDEN_INHERITANCE_ACCESS);
        }

        // PartETag 리스트 생성
        List<PartETag> partETags = request.partETags().stream()
                .map(dto -> new PartETag(dto.partNumber(), dto.eTag()))
                .collect(Collectors.toList());

        // S3에 완료 요청
        String s3ObjectKey = inheritance.getVideo().getS3ObjectKey();
        s3Service.completeMultipartUpload(s3ObjectKey, request.uploadId(), partETags);

    }


    // 상속 영상편지 삭제
    @Transactional
    public void deleteVideo(Long userId, Long inheritanceId){
        Inheritance inheritance = inheritanceRepository.findById(inheritanceId)
                .orElseThrow(() -> new CustomException(ErrorCode.INHERITANCE_NOT_FOUND));

        if(!inheritance.getUser().getUserId().equals(userId)){
            throw new CustomException(ErrorCode.FORBIDDEN_INHERITANCE_ACCESS);
        }

        InheritanceVideo video = inheritance.getVideo();
        if(video == null){
            throw new CustomException(ErrorCode.VIDEO_NOT_FOUND);
        }

        s3Service.deleteObject(video.getS3ObjectKey());
        videoRepository.delete(video);
        inheritance.setVideo(null);
    }

    // 영상편지 수신자 등록(토큰 생성 및 예약 설정)
    @Transactional
    public void registerRecipients(Long userId, Long videoId, List<RecipientRegistrationRequest> recipients){
        InheritanceVideo video = videoRepository.findById(videoId)
                .orElseThrow(() -> new CustomException(ErrorCode.VIDEO_NOT_FOUND));

        Long ownerUserId = video.getInheritance().getUser().getUserId();

        if(!ownerUserId.equals(userId)){
            throw new  CustomException(ErrorCode.FORBIDDEN_INHERITANCE_ACCESS);
        }

        // 수신자 수만큼 반복
        for(RecipientRegistrationRequest req: recipients){
            // 랜덤으로 url 뒤에 붙을 토큰 생성
            String accessLinkToken = UUID.randomUUID().toString();

            // 비디오, 이메일, 생성된 링크 토큰, 전송 예정 날짜를 포함해 레코드 생성
            InheritanceRecipient recipient = InheritanceRecipient.builder()
                    .video(video)
                    .email(req.email())
                    .accessLink(accessLinkToken)
                    .scheduledSendDate(req.scheduledSendDate())
                    .build();

            video.addRecipient(recipient);
        }
    }

    // 비회원 접근 토큰 검증 및 S3 다운로드 URL 생성
    // 수신자가 이메일을 통해 받은 URL을 클릭했을 때 유효성 검증 후 파일 볼 수 있는 임시 권한 부여
    @Transactional
    public String getPresignedUrlAndValidateToken(String token){
        InheritanceRecipient recipient = recipientRepository.findByAccessLink(token)
                .orElseThrow(() -> new CustomException(ErrorCode.INVALID_ACCESS_TOKEN));

        // 이미 사용된 링크인지 확인
        if (recipient.isLinkUsed()) {
            throw new CustomException(ErrorCode.INVALID_ACCESS_TOKEN); // 이미 사용됨
        }

        InheritanceVideo video = recipient.getVideo();
        if(video == null){
            throw new CustomException(ErrorCode.VIDEO_NOT_FOUND);
        }

        // accessLink 사용 플래그를 true로 변경
        recipient.markLinkUsed();

        // 백엔드에서 설정했던 S3에서의 경로(이름)으로 다운로드 URL 생성
        return s3Service.generateDownloadPresignedUrl(video.getS3ObjectKey());
    }
    
}