package com.know_who_how.main_server.inheritance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Part 정보 DTO
public record PartETagDTO(
        @NotNull
        @Min(1)
        int partNumber, // 조각 번호(1부터 시작)
        @NotBlank
        String eTag // S3가 Part 업로드 완료 후 반환한 ETag 값
) {}
