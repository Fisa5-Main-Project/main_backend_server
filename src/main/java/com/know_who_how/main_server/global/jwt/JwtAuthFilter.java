package com.know_who_how.main_server.global.jwt;

import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;

    // 헤더 이름
    public static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // Request Header에서 토큰 추출
        String token = resolveToken(request);

        try {
            if (token != null) {
                // JwtUtil의 validateAccessToken이 실패 시 CustomException을 던짐
                jwtUtil.validateAccessToken(token);

                // 인증 정보 SecurityContext에 저장
                Authentication authentication = jwtUtil.getAuthenticationFromAccessToken(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }
        } catch (CustomException e) {
            // JwtUtil에서 발생한 CustomException 처리
            log.warn("JWT authentication failed: {}", e.getMessage());
            request.setAttribute("exception", e.getErrorCode());
        } catch (Exception e) {
            // 알 수 없는 예외 처리
            log.error("Unexpected error during JWT authentication: {}", e.getMessage(), e);
            request.setAttribute("exception", ErrorCode.TOKEN_PARSING_FAILED); // (임시 에러 코드)
        }

        filterChain.doFilter(request, response);
    }

    // Request Header에서 토큰 정보 추출 ( "Bearer [token]" )
    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
