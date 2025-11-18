package com.know_who_how.main_server.job.controller;

import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.job.dto.JobDetailResponseDto;
import com.know_who_how.main_server.job.dto.JobListResponseDto;
import com.know_who_how.main_server.job.service.JobService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

// HTTP 요청을 받아 JobService에 전달
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/jobs")
public class JobController {
    private final JobService jobService;

    /**
     * 채용 공고 리스트 조회
     * @param search (값: "대전 중구")
     */
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
    @GetMapping("/{jobId}")
    public ApiResponse<JobDetailResponseDto> getJobDetail(@PathVariable String jobId){
        JobDetailResponseDto data = jobService.getJobDetail(jobId);
        return ApiResponse.onSuccess(data);
    }
}
