package com.know_who_how.main_server.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@RequiredArgsConstructor
public class MydataWebClientConfig {

    private final MydataProperties props;

    /*
     *   AS(토큰 엔드포인트), RS(자산 조회 API) 모두 호출부에서
     *   uri()에 완성된 절대 URL을 그대로 넘기는 방식이라 baseUrl이 필요 없어,
     *   하나의 WebClient를 공유해서 사용한다.
     */
    @Bean("mydataWebClient")
    public WebClient mydataWebClient() {
        return WebClient.builder()
                .build();
    }
}

