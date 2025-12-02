package com.know_who_how.main_server.inheritance.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

// Multipart Upload 최종 완료 요청 DTO (BE-> S3)
public record VideoUploadCompleteRequest(
        @NotBlank
        String uploadId,
        @NotEmpty
        List<@Valid PartETagDTO> partETags // 모든 조각의 ETag 정보
) {}
