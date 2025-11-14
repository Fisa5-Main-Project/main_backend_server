package com.know_who_how.main_server.auth.oauth;

import com.know_who_how.main_server.auth.oauth.dto.KakaoUserInfo;
import com.know_who_how.main_server.auth.oauth.dto.OAuthResult;
import com.know_who_how.main_server.global.util.RedisUtil;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.jwt.JwtUtil;
import com.know_who_how.main_server.user.repository.RefreshTokenRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive
        .function.client.WebClient;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OAuthService {

    private final InMemoryClientRegistrationRepository clientRegistrationRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;
    private final RefreshTokenRepository refreshTokenRepository;

    public OAuthResult oauthLogin(String registrationId, String code) {
        // 1. registrationId를 통해 ClientRegistration 정보 가져오기
        ClientRegistration provider = clientRegistrationRepository.findByRegistrationId(registrationId);

        // 2. Authorization Code로 Access Token 요청
        String accessToken = getAccessToken(provider, code);

        // 3. Access Token으로 사용자 정보 요청
        KakaoUserInfo kakaoUserInfo = getUserInfo(provider, accessToken);

        // 4. 사용자 정보로 우리 서비스의 유저를 찾거나, 없다면 신규 유저로 처리
        return processKakaoUser(kakaoUserInfo);
    }

    private String getAccessToken(ClientRegistration provider, String code) {
        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("grant_type", "authorization_code");
        formData.add("client_id", provider.getClientId());
        formData.add("redirect_uri", provider.getRedirectUri());
        formData.add("code", code);
        if (provider.getClientSecret() != null) {
            formData.add("client_secret", provider.getClientSecret());
        }

        Map<String, Object> response = WebClient.create()
                .post()
                .uri(provider.getProviderDetails().getTokenUri())
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .accept(MediaType.APPLICATION_JSON)
                .acceptCharset(StandardCharsets.UTF_8)
                .bodyValue(formData)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {})
                .block();

        return (String) response.get("access_token");
    }

    private KakaoUserInfo getUserInfo(ClientRegistration provider, String accessToken) {
        return WebClient.create()
                .get()
                .uri(provider.getProviderDetails().getUserInfoEndpoint().getUri())
                .headers(header -> header.setBearerAuth(accessToken))
                .retrieve()
                .bodyToMono(KakaoUserInfo.class)
                .block();
    }

    private OAuthResult processKakaoUser(KakaoUserInfo kakaoUserInfo) {
        String provider = "kakao";
        String providerId = kakaoUserInfo.getProviderId();

        Optional<User> userOptional = userRepository.findByProviderAndProviderId(provider, providerId);

        if (userOptional.isPresent()) {
            // 이미 가입된 유저인 경우, 로그인 처리
            User user = userOptional.get();
            log.info("기존 카카오 유저 로그인: {}", user.getLoginId());

            // JWT 토큰 발급
            String accessToken = jwtUtil.createAccessToken(user.getUserId(), user.getRoles());
            String refreshToken = jwtUtil.createRefreshToken(user.getUserId());

            // Refresh Token을 RDB에 저장/업데이트
            Instant expiryDate = jwtUtil.extractExpiration(refreshToken, true).toInstant();
            refreshTokenRepository.findByUser(user).ifPresentOrElse(
                    existingToken -> {
                        existingToken.updateToken(refreshToken, expiryDate);
                        refreshTokenRepository.save(existingToken);
                    },
                    () -> {
                        com.know_who_how.main_server.global.entity.Token.RefreshToken newRefreshToken = com.know_who_how.main_server.global.entity.Token.RefreshToken.builder()
                                .user(user)
                                .tokenValue(refreshToken)
                                .expiryDate(expiryDate)
                                .build();
                        refreshTokenRepository.save(newRefreshToken);
                    });

            return OAuthResult.builder()
                    .isNewUser(false)
                    .grantType("Bearer")
                    .accessToken(accessToken)
                    .refreshToken(refreshToken)
                    .build();
        } else {
            // 신규 유저인 경우, 회원가입 필요 응답
            log.info("신규 카카오 유저, 회원가입 필요. ProviderId: {}", providerId);

            // 1. 임시 회원가입 토큰 생성
            String signupToken = UUID.randomUUID().toString();

            // 2. Redis에 소셜 정보 저장 (10분 유효)
            String redisKey = "oauth-signup:" + signupToken;
            String redisValue = provider + ":" + providerId;
            redisUtil.save(redisKey, redisValue, Duration.ofMinutes(10));

            // 3. 클라이언트에 signupToken 반환
            return OAuthResult.builder()
                    .isNewUser(true)
                    .signupToken(signupToken)
                    .build();
        }
    }
}
