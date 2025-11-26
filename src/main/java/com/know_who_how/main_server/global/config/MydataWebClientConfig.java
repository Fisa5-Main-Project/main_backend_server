package com.know_who_how.main_server.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class MydataWebClientConfig {

    private final MydataProperties props;

    @Bean("mydataAuthWebClient")
    public WebClient mydataAuthWebClient() {
        // 지금은 tokenUri를 baseUrl로 쓰지 않고,
        // 나중에 service 쪽에서 props.getAs().getTokenUri()를 직접 uri()에 넘기는 방식으로 갈 수도 있음.
        return WebClient.builder()
                // .baseUrl(props.getAs().getTokenUri())  // 나중에 토큰 엔드포인트가 확정되면 활성화
                .build();
    }

    /*
     *   my-client-id.rs.my-data-api에 "풀 URL"이 이미 들어있으므로,
     *   여기서는 baseUrl을 설정하지 않고 builder()만 사용.
     *   실제 요청 시 MydataService에서 props.getRs().getMyDataApi()를 uri()에 그대로 넘김.
     */
    @Bean("mydataRsWebClient")
    public WebClient mydataRsWebClient() {
        return WebClient.builder()
                //.baseUrl(props.getRs().getMyDataApi())  // 예: http://localhost:8081
                .build();
    }
}

