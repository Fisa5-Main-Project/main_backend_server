package com.know_who_how.main_server.mydata.controller;

import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.mydata.service.MydataAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/my-data")
@Tag(name = "7-1. 마이데이터 연동", description = "마이데이터 연동 시작/콜백 처리 API")
public class MydataAuthController {

    // AS 연동 시작

    private final MydataAuthService mydataAuthService;

    @GetMapping("/authorize")
    @Operation(summary = "마이데이터 연동 시작",
            description = "AS 인가 서버(/oauth2/authorize)로 리다이렉트합니다.")
    public ResponseEntity<ApiResponse<?>> authorize(@AuthenticationPrincipal User user,
                          HttpServletResponse response) throws IOException {
        if (user == null) {
            throw new CustomException(ErrorCode.NOT_LOGIN_USER);
        }

        String authorizeUrl = mydataAuthService.buildAuthorizeUrl();
        log.info("마이데이터 연동 시작 - userId: {}, redirect: {}", user.getUserId(), authorizeUrl);

        return ResponseEntity.ok(ApiResponse.onSuccess(authorizeUrl));
    }

    @GetMapping("/callback")
    @Operation(summary = "마이데이터 인가코드 callback",
            description = "AS에서 전달한 authorization code로 토큰을 발급받아 저장합니다.")
    public void callback(@RequestParam("code") String code,
                         @RequestParam(value = "state", required = false) String state,
                         @AuthenticationPrincipal User user,
                         HttpServletResponse response) throws IOException{
        if (user == null) {
            // 로그인 안 된 유저가 콜백으로 들어오면 예외
            // TODO: 오류 페이지로 리다이렉트 할건지 생각
            throw new CustomException(ErrorCode.NOT_LOGIN_USER);
        }

        log.info("마이데이터 콜백 수신 - userId: {}, code: {}", user.getUserId(), code);
        mydataAuthService.handleCallback(user, code, state);

        // 프론트엔드 페이지로 리다이렉트
        String frontendUrl = "http://localhost:3000/mydata/result?status=success";
        response.sendRedirect(frontendUrl);
    }
}
