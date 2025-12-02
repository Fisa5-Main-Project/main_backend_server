package com.know_who_how.main_server.inheritance.dto;

// Part 업로드용 Presigned URL 응답 DTO (BE->FE)
public record VideoPartUrlResponse(
        int partNumber,
        String partUploadUrl
){}
