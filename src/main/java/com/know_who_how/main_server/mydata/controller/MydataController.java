package com.know_who_how.main_server.mydata.controller;

import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.dto.ErrorResponse;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.mydata.dto.MydataDto;
import com.know_who_how.main_server.mydata.service.MydataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Validated
@Tag(name = "7. 마이데이터", description = "마이데이터 자산/부채 조회 API")
public class MydataController {

    private final MydataService mydataService;

    /**
     * RS 연금 API 호출 엔드포인트.
     * 현재 로그인 사용자의 마이데이터 자산 정보로 반환한다.
     */
    @Operation(summary = "7-1. 마이데이터 조회", description = "로그인 사용자의 자산/부채를 MyData Resource Server에서 가져와 반환합니다.")
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "조회 성공",
                    content = @Content(schema = @Schema(implementation = MydataDto.class))
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "로그인 필요",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/my-data")
    public ResponseEntity<ApiResponse<MydataDto>> getMyData(@AuthenticationPrincipal User user,
                                                            HttpServletRequest request) {
        if (user == null && request.getSession(false) == null) {
            throw new CustomException(ErrorCode.NOT_LOGIN_USER);
        }
        MydataDto body = mydataService.getMyData(user, request);
        return ResponseEntity.ok(ApiResponse.onSuccess(body));
    }
}
