package com.know_who_how.main_server.job.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.know_who_how.main_server.job.dto.OpenApiJobItem;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 리스트 조회 API (getJobList)의 body.items DTO
 */
@Getter
@NoArgsConstructor
public class ExternalJobListItems {

    @JsonProperty("items")
    private ItemsWrapper items;

    @JsonProperty("totalCount")
    private int totalCount;

    @JsonProperty("pageNo")
    private int pageNo;

    // 실제 item 리스트 반환
    public List<OpenApiJobItem> getItem() {
        return (items != null) ? items.getItem() : null;
    }
}

@Getter
@NoArgsConstructor
class ItemsWrapper {
    @JsonProperty("item")
    private List<OpenApiJobItem> item; // 실제 아이템 리스트
}