package com.know_who_how.main_server.user.controller;

import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.user.dto.UserResponseDto;
import com.know_who_how.main_server.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/user")
@RequiredArgsConstructor
@Tag(name = "4. 사용자 정보 API")
public class UserController {

    private final UserService userService;

    @GetMapping("/info")
    @Operation(summary = "로그인한 사용자 정보 조회", description = "인증된 사용자의 기본 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<UserResponseDto>> getMyInfo(@AuthenticationPrincipal User user) {
        UserResponseDto userInfo = userService.getUserInfo(user);
        return ResponseEntity.ok(ApiResponse.onSuccess(userInfo));
    }
}
