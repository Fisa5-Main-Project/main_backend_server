package com.know_who_how.main_server.admin.controller;

import com.know_who_how.main_server.admin.dto.UserResponseDto;
import com.know_who_how.main_server.admin.dto.UserUpdateRequestDto;
import com.know_who_how.main_server.admin.service.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * [Admin] 사용자 정보 수정 API
     * PATCH /api/v1/admin/users/{userId}
     * 관리자 페이지에서 특정 사용자의 정보를 수정합니다 (비밀번호 초기화, 마이데이터 연동 해제 등 포함).
     */
    @PatchMapping("/users/{userId}")
    public ResponseEntity<Void> updateUser(@PathVariable Long userId, @RequestBody UserUpdateRequestDto requestDto) {
        adminUserService.updateUser(userId, requestDto);
        return ResponseEntity.ok().build();
    }
}
