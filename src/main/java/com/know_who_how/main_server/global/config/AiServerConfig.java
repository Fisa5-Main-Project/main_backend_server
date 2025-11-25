package com.know_who_how.main_server.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * AI 서버 통신을 위한 WebClient 설정
 */
@Configuration
public class AiServerConfig {

    @Value("${ai.server.url:http://localhost:8000}")
    private String aiServerUrl;

    @Bean
    public WebClient aiWebClient(WebClient.Builder builder) {
        return builder
                .baseUrl(aiServerUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }
}
