package com.know_who_how.main_server.mydata.controller;

import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.mydata.service.MydataService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
public class MydataController {

    private final MydataService mydataService;

    /**
     * RS 연금 API 호출 엔드포인트.
     * 현재 로그인 사용자의 마이데이터 연금 정보를 프록시로 반환한다.
     */
    @GetMapping("/my-data")
    public ResponseEntity<ApiResponse<?>> getMyData(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new CustomException(ErrorCode.NOT_LOGIN_USER);
        }
        String body = mydataService.getMyData();
        return ResponseEntity.ok(ApiResponse.onSuccess(body));
    }
}

