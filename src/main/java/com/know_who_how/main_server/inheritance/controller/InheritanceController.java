package com.know_who_how.main_server.inheritance.controller;

import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.inheritance.dto.*;
import com.know_who_how.main_server.inheritance.service.InheritanceService;
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
@RequiredArgsConstructor
public class InheritanceController {

    private  final InheritanceService inheritanceService;

    // --- 회원 전용 API ---

    /**
     * 상속 등록 여부 조회 API
     */
    @GetMapping("/inheritance/status")
    public ResponseEntity<ApiResponse<InheritanceStatusResponse>> getInheritanceStatus(
            @AuthenticationPrincipal Long userId){
        InheritanceStatusResponse response = inheritanceService.getInheritanceRegistrationStatus(userId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * 상속 계획 저장/업데이트 API
     */
    @PostMapping("/inheritance/plan")
    public ResponseEntity<ApiResponse<Long>> createOrUpdateInheritancePlan(
            @AuthenticationPrincipal Long userId,
            @RequestBody InheritancePlanRequest request){

        BigDecimal asset = request.asset();
        String ratio = request.ratio();

        Long inheritanceId = inheritanceService.saveOrUpdateInheritancePlan(userId, asset, ratio);

        return ResponseEntity.ok(ApiResponse.onSuccess(inheritanceId));
    }

    /**
     * [1] Multipart Upload 시작 및 Presigned URL 요청 (Initialization)
     */
    @PostMapping("/inheritance/{inheritanceId}/video/upload/init")
    public ResponseEntity<ApiResponse<VideoUploadInitResponse>> initiateVideoUpload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long inheritanceId){

        VideoUploadInitResponse response = inheritanceService.initiateVideoUpload(userId, inheritanceId);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * [2] Multipart Upload 조각(Part) 업로드용 Presigned URL 요청
     * 클라이언트가 각 청크를 업로드하기 전에 호출합니다.
     */
    @GetMapping("/inheritance/{inheritanceId}/video/upload/part")
    public ResponseEntity<ApiResponse<VideoPartUrlResponse>> getPartUploadUrl(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long inheritanceId,
            @RequestParam("uploadId") String uploadId,
            @RequestParam("partNumber") int partNumber) {

        VideoPartUrlResponse response = inheritanceService.generatePartUploadUrl(userId, inheritanceId, uploadId, partNumber);
        return ResponseEntity.ok(ApiResponse.onSuccess(response));
    }

    /**
     * [3] Multipart Upload 완료 (Completion)
     * 클라이언트가 모든 조각 업로드를 마친 후, ETag 리스트와 함께 최종 완료를 요청
     */
    @PostMapping("/inheritance/{inheritanceId}/video/upload/complete")
    public ResponseEntity<ApiResponse<Void>> completeVideoUpload(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long inheritanceId,
            @Valid @RequestBody VideoUploadCompleteRequest request) {

        inheritanceService.completeVideoUpload(userId, inheritanceId, request);
        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    /**
     * 영상편지 삭제 API
     */
    @DeleteMapping("/inheritance/{inheritanceId}/video")
    public ResponseEntity<ApiResponse<Void>> deleteVideo(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long inheritanceId){

        inheritanceService.deleteVideo(userId, inheritanceId);

        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    /**
     * 영상편지 수신자 등록
     */
    @PostMapping("/inheritance/video/{videoId}/recipients")
    public ResponseEntity<ApiResponse<Void>> registerRecipients(
            @PathVariable Long videoId,
            @RequestBody RecipientListRequest request){

        inheritanceService.registerRecipients(videoId, request.recipients());

        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    /**
     * 비회원 영상편지 조회용 API (토큰 검증 및 S3 리다이렉션)
     */
    @GetMapping("/inheritance/view-redirect")
    public ResponseEntity<Void> redirectToVideo(@RequestParam("token") String token){

        String presignedUrl = inheritanceService.getPresignedUrlAndValidateToken(token);

        HttpHeaders headers = new HttpHeaders();
        headers.setLocation(URI.create(presignedUrl));

        return new ResponseEntity<>(headers, HttpStatus.TEMPORARY_REDIRECT);
    }

}
