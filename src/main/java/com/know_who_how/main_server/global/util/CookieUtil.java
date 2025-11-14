package com.know_who_how.main_server.global.util;

import com.know_who_how.main_server.global.jwt.JwtProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CookieUtil {

    private final JwtProperties jwtProperties;

    public void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        response.addCookie(createRefreshTokenCookie(refreshToken, (int) jwtProperties.getRefreshTokenValidityInSeconds()));
    }

    public void deleteRefreshTokenCookie(HttpServletResponse response) {
        response.addCookie(createRefreshTokenCookie(null, 0));
    }

    // 공통 쿠키 생성 로직
    private Cookie createRefreshTokenCookie(String value, int maxAge) {
        Cookie cookie = new Cookie("refresh_token", value);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        return cookie;
    }
}
