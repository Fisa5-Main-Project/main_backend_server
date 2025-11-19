package com.know_who_how.main_server.global.config;

import com.know_who_how.main_server.global.entity.MyData.Mydata;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.mydata.repository.MydataRepository;
import com.know_who_how.main_server.user.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizedClientRepository;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;

/**
 * OAuth2 로그인 성공 시 Access Token /Refresh Token을 세션과 DB(Mydata)에 동기화하는 핸들러
 * - 세션: "MYDATA_ACCESS_TOKEN", "MYDATA_REFRESH_TOKEN"
 * - DB: 세션에 사용자 ID가 있는 경우(Mydata.updateTokens) 동기화 (없으면 세션만 저장)
 *
 * Spring Security의 OAuth2 Client에서는
 * "/login/oauth2/code/{registrationId}" 콜백 도착 시
 * 자동으로 OAuth2AuthorizedClient가 만들어지는데, 여기서 해당 토큰을 꺼내오는 역할도 수행함.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SavedRequestAwareAuthenticationSuccessHandler {

    /**
     * OAuth2AuthorizedClientRepository는 아래 값들을 저장할 수 있다.
     * AccessToken
     * RefreshToken
     * ExpiresAt
     * Scope
     * TokenType
     */
    private final OAuth2AuthorizedClientRepository authorizedClientRepository;
    private final MydataRepository mydataRepository;
    private final UserRepository userRepository;


    /**
     * OAuth2 로그인 성공 시 호출되는 메서드
     * OAuth2AuthorizedClient에서 AT/RT 조회
     * 세션에 AT/RT 저장
     * DB(Mydata)에 토큰/메타데이터 저장
     * User 엔티티에 “마이데이터 연동 여부” 플래그 반영
     */
    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws ServletException, IOException {
        // OAuth2AuthorizedClient에서 토큰 추출
        // OAuth2AuthenticationToken인지 확인
        if (authentication instanceof OAuth2AuthenticationToken oauth2Auth) {
            String registrationId = oauth2Auth.getAuthorizedClientRegistrationId();
            //OAuth2AuthorizedClient 가져오기
            OAuth2AuthorizedClient client = authorizedClientRepository.loadAuthorizedClient(registrationId, oauth2Auth, request);
            if (client != null && client.getAccessToken() != null) {
                // Access Token / Refresh Token 추출
                String at = client.getAccessToken().getTokenValue();
                String rt = client.getRefreshToken() != null ? client.getRefreshToken().getTokenValue() : null;

                // AccessToken 만료까지 남은 시간(초) 및 스코프 문자열 계산
                Integer expiresIn = null;
                Instant expiresAt = client.getAccessToken().getExpiresAt();
                if (expiresAt != null) {
                    long seconds = Duration.between(Instant.now(), expiresAt).getSeconds();
                    if (seconds > 0 && seconds <= Integer.MAX_VALUE) {
                        expiresIn = (int) seconds;
                    }
                }

                // AccessToken 스코프 문자열 생성
                String scope = null;
                if (client.getAccessToken().getScopes() != null && !client.getAccessToken().getScopes().isEmpty()) {
                    scope = String.join(" ", client.getAccessToken().getScopes());
                }

                // 세션 저장
                HttpSession session = request.getSession(true);
                session.setAttribute("MYDATA_ACCESS_TOKEN", at);
                if (rt != null) {
                    session.setAttribute("MYDATA_REFRESH_TOKEN", rt);
                }


                // 로그인 한 사용자 식별
                Long userId = (Long) session.getAttribute("CURRENT_USER_ID");
                if (userId == null && authentication.getPrincipal() instanceof UserDetails userDetails) {
                    String loginId = userDetails.getUsername();
                    var user = userRepository.findByLoginId(loginId)
                            .orElseThrow(() -> new CustomException(ErrorCode.NOT_LOGIN_USER));
                    userId = user.getUserId();
                }

                // DB 동기화
                if (userId != null) {
                    var userRef = userRepository.getReferenceById(userId);
                    // 사용자 마이데이터 연동 완료 플래그 설정
                    userRef.markMydataRegistered();

                    Mydata mydata = mydataRepository.findById(userId)
                            .orElseGet(() -> Mydata.builder().user(userRef).build());
                    mydata.updateTokens(at, rt);
                    mydata.updateTokenMeta(expiresIn, scope);
                    mydataRepository.save(mydata);
                }
            }
        }

        // 로딩페이지로 리다이렉트
        response.sendRedirect("http://localhost:3000/mydata/loading");
    }
}
