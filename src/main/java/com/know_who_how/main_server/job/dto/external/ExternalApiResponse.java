package com.know_who_how.main_server.job.dto.external;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.core.ParameterizedTypeReference;

// Open API의 공통 응답 래퍼 response { header, body } 매핑
@Getter
@NoArgsConstructor
public class ExternalApiResponse<T> {

    @JsonProperty("response")
    private ResponseData<T> response;

    // WebClient에서 제네릭 타입을 추론하기 위한 헬퍼 메서드
    public static ParameterizedTypeReference<ExternalApiResponse<ExternalJobListItems>> getTypeReferenceForList() {
        return new ParameterizedTypeReference<>() {};
    }

    public static ParameterizedTypeReference<ExternalApiResponse<ExternalJobDetailItemWrapper>> getTypeReferenceForDetail() {
        return new ParameterizedTypeReference<>() {};
    }

    // response 필드만 추출 (편의 메서드)
    public T getBody() {
        return (this.response != null && this.response.getBody() != null) ? this.response.getBody() : null;
    }
}

@Getter
@NoArgsConstructor
class ResponseData<T> {
    @JsonProperty("header")
    private Header header;
    @JsonProperty("body")
    private T body; // body의 타입이 리스트/상세보기에 따라 다름
}

@Getter
@NoArgsConstructor
class Header {
    @JsonProperty("resultCode")
    private String resultCode;
    @JsonProperty("resultMsg")
    private String resultMsg;
}