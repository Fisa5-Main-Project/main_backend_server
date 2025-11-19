package com.know_who_how.main_server.job.controller;

import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.job.dto.JobDetailResponseDto;
import com.know_who_how.main_server.job.dto.JobListResponseDto;
import com.know_who_how.main_server.job.service.JobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// HTTP 요청을 받아 JobService에 전달
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jobs")
@Tag(name = "6. 일자리 찾기")
public class JobController {
    private final JobService jobService;

    /**
     * 채용 공고 리스트 조회
     * @param search (값: "대전 중구")
     */
    @Operation(
            summary = "채용 공고 리스트 조회 및 검색",
            description = "지역(검색어), 고용 형태 필터, 페이지네이션 정보를 사용하여 채용 공고 목록을 조회합니다. 이 API는 Redis 캐싱을 통해 응답 속도를 최적화합니다."
    )
    @GetMapping
    public ApiResponse<JobListResponseDto> getJobs(
            @RequestParam String search,
            @RequestParam String employmentType,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size
    ){
        JobListResponseDto data = jobService.getJobList(search, employmentType,page, size);
        return ApiResponse.onSuccess(data);
    }

    /**
     * 채용 공고 상세 조회
     */
    @Operation(
            summary = "특정 채용 공고 상세 정보 조회",
            description = "고유 Job ID를 사용하여 상세 정보를 조회합니다. 리스트 조회 시 캐시된 '고용 형태' 및 '직종' 정보와 상세 API 응답을 조합하여 제공합니다."
    )
    @GetMapping("/{jobId}")
    public ApiResponse<JobDetailResponseDto> getJobDetail(@PathVariable String jobId){
        JobDetailResponseDto data = jobService.getJobDetail(jobId);
        return ApiResponse.onSuccess(data);
    }
}
