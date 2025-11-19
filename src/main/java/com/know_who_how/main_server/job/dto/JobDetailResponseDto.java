package com.know_who_how.main_server.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// 채용 공고 상세 정보 반환 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobDetailResponseDto {
    private String id;
    private String title;
    private String companyName;
    private String location;
    private String employmentType;
    private String deadlineStatus; // 오늘 마감, 접수중, 접수 마감
    private String endDate; // 종료 접수일
    private String jobCategory;
    private String description;
    private String applyMethod;
    private String homepageUrl;
}
