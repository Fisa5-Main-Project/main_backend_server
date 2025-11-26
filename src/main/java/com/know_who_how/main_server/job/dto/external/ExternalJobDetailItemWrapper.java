package com.know_who_how.main_server.job.dto.external;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.know_who_how.main_server.job.dto.OpenApiDetailItem;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 상세 조회 API (getJobInfo)의 body DTO
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@NoArgsConstructor
public class ExternalJobDetailItemWrapper {

    @JsonProperty("items")
    private DetailItemsWrapper items;

    /**
     * JobService에서 detailItem을 쉽게 꺼내기 위한 헬퍼 메서드
     * (body.items.item)
     */
    public OpenApiDetailItem getItem(){
        return (items !=null) ? items.getItem(): null;
    }

}

// <items> 래퍼 내부의 <item>를 매핑하기 위한 내부 클래스
@Getter
@NoArgsConstructor
class DetailItemsWrapper {
    @JsonProperty("item")
    private OpenApiDetailItem item; // 상세 조회의 item은 단일 객체
}