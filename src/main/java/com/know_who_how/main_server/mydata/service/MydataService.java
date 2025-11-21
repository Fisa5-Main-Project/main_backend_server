package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.config.MydataProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

@Service
@RequiredArgsConstructor
public class MydataService {

    private final MydataProperties mydataProps;
    // OAuth2 Client(WebClient) 기반 RS 호출용
    // 프론트에서 apiClient.get('mydata/pension') 호출 받고 RS에 GET요청
    private final WebClient mydataWebClient;

    /**
     * RS 연금 API 호출 로직.
     * DB에 저장된 Access Token을 직접 사용하지 않고,
     * OAuth2 Client(WebClient)를 통해 RS를 프록시 호출한다.
     */
    @Transactional(readOnly = true)
    public String getMyData() {
        String url = mydataProps.getRs().getMyDataApi();
        return mydataWebClient
                .get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }
}

