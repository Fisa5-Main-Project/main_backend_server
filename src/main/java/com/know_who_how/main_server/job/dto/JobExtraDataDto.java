package com.know_who_how.main_server.job.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

/**
 * 상세보기 API에 없는 '고용형태', '직종' 데이터를
 * 리스트 API에서 조회하여 Redis에 캐시하기 위한 DTO
 */
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExtraDataDto implements Serializable {
    private String employmentType; // 고용형태 (emplymShpNm)
    private String jobCategory;    // 직종 (jobclsNm)
}