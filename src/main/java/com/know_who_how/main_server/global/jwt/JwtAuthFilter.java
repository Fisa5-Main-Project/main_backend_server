package com.know_who_how.main_server.global.jwt;

import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.global.util.RedisUtil;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtUtil jwtUtil;
    private final RedisUtil redisUtil;

    public static final String AUTHORIZATION_HEADER = "Authorization";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);

            if (token != null) {
                // 블랙리스트(로그아웃) 토큰 확인
                if (redisUtil.get(token) != null) {
                    throw new CustomException(ErrorCode.ALREADY_LOGGED_OUT);
                }

                // JWT 검증 및 인증 세팅
                jwtUtil.validateAccessToken(token);
                Authentication authentication = jwtUtil.getAuthenticationFromAccessToken(token);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } else {
                // JWT 헤더 없고 OAuth2 세션이 있으면 그대로 통과 (마이데이터 OAuth2 유지)
                Authentication currentAuth = SecurityContextHolder.getContext().getAuthentication();
                if (currentAuth instanceof OAuth2AuthenticationToken) {
                    filterChain.doFilter(request, response);
                    return;
                }
            }
        } catch (CustomException e) {
            request.setAttribute("exception", e.getErrorCode());
        } catch (SecurityException | MalformedJwtException e) {
            request.setAttribute("exception", ErrorCode.INVALID_TOKEN_SIGNATURE);
        } catch (ExpiredJwtException e) {
            request.setAttribute("exception", ErrorCode.TOKEN_EXPIRED);
        } catch (UnsupportedJwtException e) {
            request.setAttribute("exception", ErrorCode.UNSUPPORTED_TOKEN_ERROR);
        } catch (IllegalArgumentException e) {
            request.setAttribute("exception", ErrorCode.TOKEN_PARSING_FAILED);
        }

        filterChain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest request) {
        String bearerToken = request.getHeader(AUTHORIZATION_HEADER);
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
