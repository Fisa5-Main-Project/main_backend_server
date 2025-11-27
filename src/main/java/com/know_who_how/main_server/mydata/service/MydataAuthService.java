package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.config.MydataProperties;
import com.know_who_how.main_server.global.entity.Mydata.Mydata;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.util.RedisUtil;
import com.know_who_how.main_server.mydata.repository.MydataRepository;
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
    private final WebClient mydataAuthWebClient; // @Qualifier 생략: 해당 타입 빈이 하나뿐이면 자동 주입

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

        // state는 보안상 사용하는 것이 좋음 (여기서는 TODO 수준으로 남겨둠)
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
    public void handleCallback(User user, String code, String state) {
        if (user == null) {
            throw new CustomException(ErrorCode.NOT_LOGIN_USER);
        }

        // TODO: state 검증 로직(보안용) 필요시 추가 가능

        // Code로 토큰 교환
        Map<String, Object> tokenResponse = exchangeCodeForToken(code);

        // Token 저장
        saveTokens(user, tokenResponse);
    }

    /**
     * Authorization Code → Access/Refresh Token 교환
     */
    private Map<String, Object> exchangeCodeForToken(String code) {
        String tokenUri = mydataProperties.getAs().getTokenUri();
        if (tokenUri == null || tokenUri.isBlank()) {
            log.error("MyData tokenUri 설정이 없습니다.");
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        }

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("code", code);
        formData.add("redirect_uri", mydataProperties.getRedirectUri());

        try {
            return mydataAuthWebClient.post()
                    .uri(tokenUri)
                    // Basic Auth 헤더 추가
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
            log.error("MyData 토큰 교환 실패 - status: {}, body: {}",
                    e.getStatusCode(), e.getResponseBodyAsString());
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        } catch (Exception e) {
            log.error("MyData 토큰 교환 중 예외 발생", e);
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        }
    }

    /**
     * 3) 토큰 응답을 파싱하여 Mydata 엔티티에 저장/갱신
     */
    private void saveTokens(User user, Map<String, Object> tokenResponse) {
        if (tokenResponse == null) {
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        }

        String accessToken = (String) tokenResponse.get("access_token");
        String refreshToken = (String) tokenResponse.get("refresh_token");

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
        String redisKey = "mydata:access:" + user.getUserId();
        // 만료 시간이 없으면 기본 1시간(3600초) 설정
        long ttlSeconds = (expiresIn != null) ? expiresIn : 3600;

        // accessToken redis에 저장
        redisUtil.save(redisKey, accessToken, Duration.ofSeconds(ttlSeconds));
        log.info("Redis 저장 완료: Key={}, TTL={}초", redisKey, ttlSeconds);

        // scope 검증
        String scope = (String) tokenResponse.get("scope");
        if(scope == null || scope.isBlank()) {
            log.error("MyData 토큰 응답에 scope 값이 없습니다. 응답: {}", tokenResponse);
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        }

        mydataRepository.findById(user.getUserId())
                .ifPresentOrElse(
                        // 이미 연동된 유저라면 -> Refresh Token만 업데이트 (Dirty Checking)
                        existingData -> {
                            existingData.updateRefreshToken(refreshToken);
                            log.info("RDB 업데이트 완료: UserID={}", user.getUserId());
                        },
                        // 첫 연동이라면 -> insert
                        () -> {
                            mydataRepository.save(Mydata.builder()
                                    .user(user)
                                    .refreshToken(refreshToken)
                                    .scope(scope)
                                    .build());
                            log.info("RDB 신규 저장 완료: UserID={}", user.getUserId());
                        }
                );
    }
}
