package com.know_who_how.main_server.admin.controller;

import com.know_who_how.main_server.admin.dto.UserResponseDto;
import com.know_who_how.main_server.admin.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * [Admin] 사용자 관리 API
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * [7] 전체 사용자 목록 조회 API
     * GET /api/v1/admin/users
     * 관리자 페이지의 사용자 관리 탭에서 사용하는 전체 사용자 목록을 조회합니다.
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserResponseDto>> getUsers() {
        List<UserResponseDto> users = adminUserService.getUsers();
        return ResponseEntity.ok(users);
    }
}
