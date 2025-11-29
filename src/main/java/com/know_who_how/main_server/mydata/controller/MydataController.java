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

@RestController
@RequestMapping("/api/v1/resource")
@RequiredArgsConstructor
@Validated
@Tag(name = "7. 마이데이터", description = "마이데이터 자산/부채 조회 API")
public class MydataController {

    private final MydataService mydataService;

    /**
     * RS 연금/자산 API 호출 엔드포인트.
     * 현재 로그인 사용자의 마이데이터 자산 정보로 반환한다.
     */
    @Operation(
            summary = "마이데이터 자산/부채 통합 조회",
            description = "RS(MyData API)의 /api/v1/my-data를 호출하여 자산/부채/연금 정보를 가져옵니다."
    )
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
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "409",
                    description = "마이데이터 미연동",
                    content = @Content(schema = @Schema(implementation = ErrorResponse.class))
            )
    })
    @GetMapping("/my-data")
    public ResponseEntity<ApiResponse<MydataDto>> getMyData(@AuthenticationPrincipal User user) {
        if (user == null) {
            throw new CustomException(ErrorCode.NOT_LOGIN_USER);
        }
        MydataDto body = mydataService.getMyData(user);
        return ResponseEntity.ok(ApiResponse.onSuccess(body));
    }
}
