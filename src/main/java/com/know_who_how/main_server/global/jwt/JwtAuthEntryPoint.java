package com.know_who_how.main_server.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAuthEntryPoint implements AuthenticationEntryPoint {
    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException, ServletException {
        // 필터에서 발생한 예외를 request attribute에서 가져옴
        ErrorCode errorCode = (ErrorCode) request.getAttribute("exception");

        // 예외가 없는 경우, 일반적인 인증 실패로 처리
        if (errorCode == null) {
            errorCode = ErrorCode.NOT_LOGIN_USER;
        }

        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<?> errorResponse = ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage());
        String json = new ObjectMapper().writeValueAsString(errorResponse);
        response.getWriter().write(json);
    }

}

