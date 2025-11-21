package com.know_who_how.main_server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2AuthorizeRequest;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClientProviderBuilder;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServletOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * RS 호출 시 Access Token 사용 및 Refresh Token으로 재발급하기 위한 공통 OAuth2 Client 설정
 * 만약 401이 반환되면 refreshToken으로 재발급 후 1회 재시도할 수 있도록 구성
 * 흐름
 * 메인 서버(BFF) → (AT 자동 주입) → 리소스 서버(RS)
 * RS가 401 반환 → refresh_token으로 재발급 → 동일 요청 1회 재전송
 */
@Configuration
public class OAuth2ClientConfig {

    // authorization_code, refresh_token 플로우 모두 지원하는 OAuth2 Client 관리자.
    // AccessToken이 만료되었을 때 refreshToken으로 새 AT/RT를 재발급할 때 사용됨.
    // WebClient의 OAuth2 필터(ServletOAuth2AuthorizedClientExchangeFilterFunction)과 함께 사용됨.
    @Bean
    public OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager(
            ClientRegistrationRepository clientRegistrationRepository,
            OAuth2AuthorizedClientRepository authorizedClientRepository
    ) {
        OAuth2AuthorizedClientProvider provider = OAuth2AuthorizedClientProviderBuilder.builder()
                .authorizationCode() // code → token 교환 플로우
                .refreshToken() // // refresh-token → 새 token 발급 플로우
                .build();

        // ClientManager 생성
        DefaultOAuth2AuthorizedClientManager manager =
                new DefaultOAuth2AuthorizedClientManager(
                        clientRegistrationRepository,
                        authorizedClientRepository
                );
        // 위 provider 설정을 manager에게 연결
        manager.setAuthorizedClientProvider(provider);
        return manager;
    }

    /**
     * MyData API 호출 전용 WebClient Bean
     * BFF(Server)에서 RS(MyData API Server) 호출 시 AccessToken 자동 주입
     * 401 응답 시 refresh_token으로 재발급 후 1회 재시도
     */
    @Bean
    public WebClient mydataWebClient(
            OAuth2AuthorizedClientManager oAuth2AuthorizedClientManager,
            MydataProperties mydataProperties
    ) {
        int timeoutMs = mydataProperties.getTimeoutMs();

        // Netty HttpClient에 타임아웃 적용
        HttpClient httpClient = HttpClient.create()
                .responseTimeout(Duration.ofMillis(timeoutMs));

        /**
         * 세션에 저장된 OAuth2AuthorizedClient에서 직접 Token값을 조회한다
         *
         * 현재 SecurityContextHolder 인증 정보 기반으로
         * 등록된 clientRegistrationId("my-client-id")의 AccessToken을 찾아
         * 자동으로 Authorization: Bearer <token> 헤더를 붙여줌
         */
        ServletOAuth2AuthorizedClientExchangeFilterFunction oauth2Filter =
                new ServletOAuth2AuthorizedClientExchangeFilterFunction(oAuth2AuthorizedClientManager);
        oauth2Filter.setDefaultClientRegistrationId("my-client-id");

        // 401 응답 시 refresh_token으로 재발급 후 동일 요청 1회 재시도
        ExchangeFilterFunction refreshOnUnauthorized = (request, next) ->
                next.exchange(request).flatMap((ClientResponse response) -> {
                    if (response.statusCode().value() != 401) {
                        return Mono.just(response);
                    }

                    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
                    if (authentication == null) {
                        // 인증 정보가 없으면 재발급 시도 없이 그대로 401 반환
                        return Mono.just(response);
                    }

                    OAuth2AuthorizeRequest authorizeRequest = OAuth2AuthorizeRequest
                            .withClientRegistrationId("my-client-id")
                            .principal(authentication)
                            .build();
                    oAuth2AuthorizedClientManager.authorize(authorizeRequest);

                    // 새 토큰이 세션/AuthorizedClientRepository에 반영되었으므로 동일 요청을 한 번만 재시도
                    return next.exchange(request);
                });

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .apply(oauth2Filter.oauth2Configuration())
                .filter(refreshOnUnauthorized)
                .build();
    }
}