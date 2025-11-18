package com.know_who_how.main_server.job.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Open API 목록 조회 응답(item 배열)의 개별 항목 매핑 DTO
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
public class OpenApiJobItem {

    @JsonProperty("jobId")
    private String jobId;

    @JsonProperty("recrtTitle")
    private String recrtTitle; // 채용 제목

    @JsonProperty("oranNm")
    private String oranNm; // 기업명

    @JsonProperty("workPlcNm")
    private String workPlcNm; // 근무지명(위치)

    @JsonProperty("deadline")
    private String deadline; // 마감 여부(마감 or 접수중)

    @JsonProperty("emplymShpNm")
    private String emplymShpNm; // 근무형태명 (정규직/계약직/시간제일자리/일당직/기타)

    @JsonProperty("toDd")
    private String toDd; // 종료접수일(yyyyMMdd)

    @JsonProperty("jobclsNm")
    private String jobclsNm; // 직종명
}