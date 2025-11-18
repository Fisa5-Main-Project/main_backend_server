package com.know_who_how.main_server.job.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;

// Open API 상세 조회 응답(item 객체) 매핑 DTO
@Getter
@NoArgsConstructor
public class OpenApiDetailItem {

    // 고용 형태(emplymShpNm)와 직종(jobclsNm)은
    // JobService에서 Redis(JobExtraDataDto)를 통해 별도로 가져와 조합

    @JsonProperty("jobId")
    private String jobId; // 구인인증번호 (wantedAuthNo와 동일)

    @JsonProperty("plDetAddr")
    private String plDetAddr; // 근무 상세 위치 (oo시 oo구 oo번길 ~)

    @JsonProperty("wantedTitle")
    private String wantedTitle; // 채용제목

    @JsonProperty("plbizNm")
    private String plbizNm; // 사업장명 (ex. 마이비산부인과)

    @JsonProperty("toAcptDd")
    private String toAcptDd; // 종료접수일 (ex. 20150724)

    @JsonProperty("detCnts")
    private String detCnts; // 상세내용

    @JsonProperty("homepage")
    private String homepage; // 홈페이지 링크

    @JsonProperty("acptMthdCd")
    private String acptMthdCd; // 접수방법 (CM0801:온라인)
}