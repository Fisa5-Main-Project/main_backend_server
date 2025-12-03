package com.know_who_how.main_server.global.jwt;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.util.RedisUtil; // New import
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
import org.slf4j.MDC;
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
    private final RedisUtil redisUtil; // Replaced RedisTemplate with RedisUtil

    // 헤더 이름
    public static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        // 관리자 API 경로는 토큰 검증을 임시로 건너뜀
        if (request.getRequestURI().startsWith("/api/v1/admin/")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = resolveToken(request);

        try {
            if (token != null) {
                try {
                    // Redis 블랙리스트 확인: 이미 로그아웃된 토큰인지 검사
                    if (redisUtil.get(token) != null) { // Use redisUtil.get
                        throw new CustomException(ErrorCode.ALREADY_LOGGED_OUT);
                    }

                    // 토큰 검증
                    jwtUtil.validateAccessToken(token);

                    // 인증 정보 SecurityContext에 저장
                    Authentication authentication = jwtUtil.getAuthenticationFromAccessToken(token);
                    SecurityContextHolder.getContext().setAuthentication(authentication);

                    // MCD에 userId 추가
                    User user = (User) authentication.getPrincipal();
                    MDC.put("ActiveUserId", user.getUserId().toString());

                    log.info("MDC set for user: {}", user.getUserId()); // 임시 로그 추가

                } catch (CustomException e) {
                    request.setAttribute("exception", e.getErrorCode());
                    MDC.put("errorCode", e.getErrorCode().getCode());
                    MDC.put("errorMessage", e.getErrorCode().getMessage());
                } catch (SecurityException | MalformedJwtException e) {
                    request.setAttribute("exception", ErrorCode.INVALID_TOKEN_SIGNATURE);
                    MDC.put("errorCode", ErrorCode.INVALID_TOKEN_SIGNATURE.getCode());
                    MDC.put("errorMessage", ErrorCode.INVALID_TOKEN_SIGNATURE.getMessage());
                } catch (ExpiredJwtException e) {
                    request.setAttribute("exception", ErrorCode.TOKEN_EXPIRED);
                    MDC.put("errorCode", ErrorCode.TOKEN_EXPIRED.getCode());
                    MDC.put("errorMessage", ErrorCode.TOKEN_EXPIRED.getMessage());
                } catch (UnsupportedJwtException e) {
                    request.setAttribute("exception", ErrorCode.UNSUPPORTED_TOKEN_ERROR);
                    MDC.put("errorCode", ErrorCode.UNSUPPORTED_TOKEN_ERROR.getCode());
                    MDC.put("errorMessage", ErrorCode.UNSUPPORTED_TOKEN_ERROR.getMessage());
                } catch (IllegalArgumentException e) {
                    request.setAttribute("exception", ErrorCode.TOKEN_PARSING_FAILED);
                    MDC.put("errorCode", ErrorCode.TOKEN_PARSING_FAILED.getCode());
                    MDC.put("errorMessage", ErrorCode.TOKEN_PARSING_FAILED.getMessage());
                }
            }
            filterChain.doFilter(request, response);

        } finally {
            // 요청 처리 완료 후 MCD에서 제거
            MDC.remove("ActiveUserId");
            MDC.remove("errorCode");
            MDC.remove("errorMessage");
        }
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
