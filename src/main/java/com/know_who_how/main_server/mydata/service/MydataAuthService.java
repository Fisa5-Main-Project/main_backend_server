package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.config.MydataProperties;
import com.know_who_how.main_server.global.entity.Mydata.Mydata;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.util.RedisUtil;
import com.know_who_how.main_server.mydata.repository.MydataRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class MydataAuthService {

    // AS와 코드 교환 + 토큰 저장

    private final MydataProperties mydataProperties;
    private final MydataRepository mydataRepository;
    private final WebClient mydataAuthWebClient;
    private final UserRepository userRepository;

    private final RedisUtil redisUtil;

    /**
     * 1) 연동 시작 시, AS의 authorize URL을 만들어 반환한다.
     *    컨트롤러에서 이 URL로 리다이렉트.
     */
    public String buildAuthorizeUrl() {
        String authorizeUri = mydataProperties.getAs().getAuthorizeUri();
        if (authorizeUri == null || authorizeUri.isBlank()) {
            log.error("MyData authorizeUri 설정이 없습니다.");
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        }

        // scope는 필요 시 config로 뺄 수 있음
        // 기존 방식에선 scope yml 설정에 포함되었음
        String scope = "openid my.data.read";

        // state는 보안상 사용하는 것이 좋음
        String state = "dummy-state"; // TODO: CSRF 방지용 state 생성/검증 로직 추가

        String url = authorizeUri
                + "?response_type=code"
                + "&client_id=" + mydataProperties.getClientId()
                + "&scope=" + scope.replace(" ", "%20") // 공백문자 방지용 URL 인코딩
                + "&state=" + state
                + "&redirect_uri=" + mydataProperties.getRedirectUri();

        log.debug("MyData authorize URL 생성: {}", url);
        return url;
    }

    /**
     * 2) AS에서 code를 받아오면, /oauth2/token으로 교환하여
     *    Access/Refresh Token을 Mydata 테이블에 저장한다.
     */
    @Transactional
    public void handleCallback(Long userId, String code, String state) {
        if (userId == null) {
            throw new CustomException(ErrorCode.NOT_LOGIN_USER);
        }

        // TODO: state 검증 로직(보안용) 필요시 추가 가능

        // Code로 토큰 교환
        Map<String, Object> tokenResponse = exchangeCodeForToken(code);

        // Token 저장
        processTokenResponse(userId, tokenResponse);
    }

    /**
     * Authorization Code → Access/Refresh Token 교환
     */
    @Transactional
    public Map<String, Object> exchangeCodeForToken(String code) {
        String tokenUri = mydataProperties.getAs().getTokenUri();
        if (tokenUri == null || tokenUri.isBlank()) {
            log.error("MyData tokenUri 설정이 없습니다.");
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        }

        log.info("token uri : {}, code : {}", tokenUri, code);
        log.info("Auth 서버로 보내는 Redirect URI: {}", mydataProperties.getRedirectUri());

        // Form Data 구성
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", mydataProperties.getRedirectUri());

        // 공통 메서드 호출 (4xx 에러 시 SERVER_ERROR 발생시킴 - 기존 로직 유지)
        return sendTokenRequest(formData, ErrorCode.MYDATA_SERVER_ERROR);
    }

    /**
     * 3) 토큰 응답을 파싱하여 Mydata 엔티티에 저장/갱신
     */
    private void processTokenResponse(Long userId, Map<String, Object> tokenResponse) {
        if (tokenResponse == null) {
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        }

        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");
        String scope = (String) tokenResponse.get("scope");

        // accessToken 검증
        if (accessToken == null || accessToken.isBlank()) {
            log.error("MyData 토큰 응답에 access_token이 없습니다. 응답: {}", tokenResponse);
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        }

        // expire 검증
        Integer expiresIn = null;
        if (tokenResponse.get("expires_in") instanceof Number num) {
            expiresIn = num.intValue();
        }

        // redis 키 생성
        String redisKey = "mydata:access:" + userId;
        // 만료 시간이 없으면 기본 1시간(3600초) 설정
        long ttlSeconds = (expiresIn != null) ? expiresIn : 3600;

        // accessToken redis에 저장
        redisUtil.save(redisKey, accessToken, Duration.ofSeconds(ttlSeconds));
        log.info("Redis 저장 완료: Key={}, TTL={}초", redisKey, ttlSeconds);

        mydataRepository.findById(userId)
                .ifPresentOrElse(
                        // 이미 연동된 유저 -> Refresh Token 업데이트
                        existingData -> {
                            existingData.updateRefreshToken(refreshToken);
                            log.info("RDB 업데이트 완료: UserID={}", userId);
                        },
                        // 첫 연동 -> 신규 저장
                        () -> {
                            // 신규 저장 시에는 Scope와 RefreshToken이 필수
                            if (scope == null || scope.isBlank()) {
                                log.error("신규 연동인데 scope가 없습니다. UserID: {}", userId);
                                throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
                            }

                            // Proxy 객체 사용으로 SELECT 쿼리 절약
                            User userRef = userRepository.getReferenceById(userId);

                            mydataRepository.save(Mydata.builder()
                                    .user(userRef)
                                    .refreshToken(refreshToken)
                                    .scope(scope)
                                    .build());
                            log.info("RDB 신규 저장 완료: UserID={}", userId);
                        }
                );
    }

    @Transactional
    public String refreshAccessToken(Long userId) {
        // DB 검증 및 Refresh Token 조회
        Mydata mydata = mydataRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.MYDATA_NOT_LINKED));

        String currentRefreshToken = mydata.getRefreshToken();
        if (currentRefreshToken == null || currentRefreshToken.isBlank()) {
            throw new CustomException(ErrorCode.MYDATA_EXPIRED);
        }

        // Form Data 구성
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "refresh_token");
        formData.add("refresh_token", currentRefreshToken);
        // 필요한 경우 redirect_uri 추가

        // 공통 메서드 호출 (4xx 에러 시 MYDATA_EXPIRED 발생시킴 - 재로그인 유도)
        Map<String, Object> tokenResponse = sendTokenRequest(formData, ErrorCode.MYDATA_EXPIRED);

        processTokenResponse(userId, tokenResponse);

        return (String) tokenResponse.get("access_token");
    }

    private Map<String, Object> sendTokenRequest(MultiValueMap<String, String> formData, ErrorCode on4xxError) {
        String tokenUri = mydataProperties.getAs().getTokenUri();

        try {
            return mydataAuthWebClient.post()
                    .uri(tokenUri)
                    .headers(headers -> headers.setBasicAuth(
                            mydataProperties.getClientId(),
                            mydataProperties.getClientSecret()
                    ))
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .accept(MediaType.APPLICATION_JSON)
                    .acceptCharset(StandardCharsets.UTF_8)
                    .bodyValue(formData)
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                    .block();

        } catch (WebClientResponseException e) {
            log.error("MyData 토큰 요청 실패 - status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());

            // 4xx 에러(클라이언트 잘못 or 만료)일 때 던질 예외를 파라미터로 받아서 처리
            if (e.getStatusCode().is4xxClientError()) {
                throw new CustomException(on4xxError);
            }
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);

        } catch (Exception e) {
            log.error("MyData 토큰 요청 중 시스템 오류 발생", e);
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        }
    }
}
