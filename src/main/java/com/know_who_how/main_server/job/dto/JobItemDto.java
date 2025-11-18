package com.know_who_how.main_server.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

// JobListResponseDto에 포함될 개별 채용 공고의 요약 정보
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobItemDto {
    private String id;
    private String title;
    private String companyName;
    private String location;
    private String deadlineStatus;
    private String employmentType;
    private String jobCategory;
}
