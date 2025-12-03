package com.know_who_how.main_server.inheritance.dto;

// Multipart 업로드 시작 시 응답 DTO (BE -> FE)
// upload 할 때 필요한 정보들
public record VideoUploadInitResponse (
        Long inheritanceId,
        Long videoId,
        String uploadId, // S3 Upload 식별자
        String s3ObjectKey // S3 저장 경로
){}
