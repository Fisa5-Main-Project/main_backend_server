package com.know_who_how.main_server.global.jwt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.exception.ErrorCode;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
public class JwtAccessDeniedHandler implements AccessDeniedHandler {
    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException, ServletException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        // 에러코드 403
        ErrorCode errorCode = ErrorCode.ACCESS_DENIED;
        ApiResponse<?> errorResponse = ApiResponse.onFailure(errorCode.getCode(), errorCode.getMessage());
        String json = new ObjectMapper().writeValueAsString(errorResponse);
        response.getWriter().write(json);
    }

}
