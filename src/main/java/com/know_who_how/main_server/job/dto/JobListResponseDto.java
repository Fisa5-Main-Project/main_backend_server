package com.know_who_how.main_server.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

// 채용 공고 목록 반환 시 pagination 정보와 jobs 리스트 담는 DTO
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobListResponseDto {
    private PaginationDto pagination;
    private List<JobItemDto> jobs;
}
