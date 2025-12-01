package com.know_who_how.main_server.inheritance.controller;

import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.entity.User.User; // User 엔티티 import 추가
import com.know_who_how.main_server.inheritance.dto.*;
import com.know_who_how.main_server.inheritance.service.InheritanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.net.URI;

@RestController
@RequestMapping("/api/v1/inheritance")
@RequiredArgsConstructor
@Tag(name="8. 상속")
public class InheritanceController {

    private final InheritanceService inheritanceService;

    // --- 회원 전용 API ---

    /**
     * 상속 등록 여부 조회 API
     */
    @Operation(
            summary = "상속 등록 여부 조회",
            description = "현재 사용자가 상속 설계를 받았는지 확인합니다. 결과에 따라 프론트 라우팅 위치가 달라집니다."
    )
    @GetMapping("/status")
    public ResponseEntity<ApiResponse<InheritanceStatusResponse>> getInheritanceStatus(
            @AuthenticationPrincipal User user){

        Long userId = user.getUserId();

        InheritanceStatusResponse response = inheritanceService.getInheritanceRegistrationStatus(userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 상속 계획 저장/업데이트 API
     */
    @Operation(summary = "상속 자산 및 비율 저장/업데이트", description = "상속 설계를 확정하고 DB에 저장하며, User의 상속 등록 상태를 활성화합니다.")
    @PostMapping("/plan")
    public ResponseEntity<ApiResponse<InheritanceIdResponse>> createOrUpdateInheritancePlan(
            @AuthenticationPrincipal User user,
            @RequestBody InheritancePlanRequest request){

        Long userId = user.getUserId();
        BigDecimal asset = request.asset();
        String ratio = request.ratio();

        Long inheritanceId = inheritanceService.saveOrUpdateInheritancePlan(userId, asset, ratio);

        return ResponseEntity.ok(ApiResponse.onSuccess(new InheritanceIdResponse(inheritanceId)));
    }

    /**
     * [1] Multipart Upload 시작 및 Presigned URL 요청 (Initialization)
     */
    @Operation(summary = "영상 업로드 시작", description = "S3 Multipart Upload를 시작하고 Upload ID 및 Presigned URL 요청에 필요한 정보를 받습니다.")
    @PostMapping("/{inheritanceId}/video/upload/init")
    public ResponseEntity<ApiResponse<VideoUploadInitResponse>> initiateVideoUpload(
            @AuthenticationPrincipal User user,
            @PathVariable Long inheritanceId){

        Long userId = user.getUserId();

        VideoUploadInitResponse response = inheritanceService.initiateVideoUpload(userId, inheritanceId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * [2] Multipart Upload 조각(Part) 업로드용 Presigned URL 요청
     */
    @Operation(summary = "영상 조각 업로드 URL 요청", description = "특정 조각(Part)을 S3에 PUT할 수 있는 Presigned URL을 반환합니다.")
    @GetMapping("/{inheritanceId}/video/upload/part")
    public ResponseEntity<ApiResponse<VideoPartUrlResponse>> getPartUploadUrl(
            @AuthenticationPrincipal User user,
            @PathVariable Long inheritanceId,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") int partNumber) {

        Long userId = user.getUserId();

        VideoPartUrlResponse response = inheritanceService.generatePartUploadUrl(userId, inheritanceId, uploadId, partNumber);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * [3] Multipart Upload 완료 (Completion)
     */
    @Operation(summary = "Multipart Upload 최종 완료", description = "모든 조각의 ETag 정보를 받아 S3에 최종적으로 파일 합치기를 명령합니다.")
    @PostMapping("/{inheritanceId}/video/upload/complete")
    public ResponseEntity<ApiResponse<Void>> completeVideoUpload(
            @AuthenticationPrincipal User user,
            @PathVariable Long inheritanceId,
            @Valid @RequestBody VideoUploadCompleteRequest request) {

        Long userId = user.getUserId();

        inheritanceService.completeVideoUpload(userId, inheritanceId, request);
        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    /**
     * 영상편지 삭제 API
     */
    @Operation(summary = "영상편지 삭제", description = "상속 계획에 연결된 영상 파일을 S3와 DB에서 완전히 삭제합니다. (소유자만 가능)")
    @DeleteMapping("/{inheritanceId}/video")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(
            @AuthenticationPrincipal User user,
            @PathVariable Long inheritanceId){

        Long userId = user.getUserId();

        inheritanceService.deleteVideo(userId, inheritanceId);

        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    /**
     * 영상편지 수신자 등록
     */
    @Operation(summary = "영상편지 수신자 및 예약 등록", description = "업로드된 영상에 대해 수신자 목록과 발송 예약 시점을 등록합니다. 각 수신자별 고유 토큰(accessLink)이 생성됩니다. (소유자만 가능)")
    @PostMapping("/video/{videoId}/recipients")
    public ResponseEntity<ApiResponse<Void>> registerRecipients(
            @AuthenticationPrincipal User user,
            @PathVariable Long videoId,
            @RequestBody RecipientListRequest request){

        Long userId = user.getUserId();

        inheritanceService.registerRecipients(userId, videoId, request.recipients());

        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    /**
     * 비회원 영상편지 조회용 API (토큰 검증 및 S3 리다이렉션)
     */
    @Operation(summary = "비회원 영상 조회 (리다이렉트)", description = "수신자가 이메일로 받은 토큰을 검증하고, S3 영상으로 리다이렉트합니다. (인증 불필요)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "307", description = "S3 Presigned URL로 리다이렉트")
    @GetMapping("/video-letter")
    public ResponseEntity<Void> redirectToVideo(@RequestParam("token") String token){

        String presignedUrl = inheritanceService.getPresignedUrlAndValidateToken(token);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(presignedUrl));

        return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
    }
}