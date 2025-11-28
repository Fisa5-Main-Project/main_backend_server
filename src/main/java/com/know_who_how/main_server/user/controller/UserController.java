package com.know_who_how.main_server.user.controller;

import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.dto.ErrorResponse;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.user.dto.*;
import com.know_who_how.main_server.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

    @PatchMapping("/profile")
    @Operation(summary = "사용자 프로필 정보 수정", description = "로그인한 사용자의 프로필 정보를 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "프로필 정보 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 유효성 검사 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ProfileUpdateRequestDto requestDto) {
        userService.updateProfile(user, requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess("프로필 정보가 성공적으로 업데이트되었습니다."));
    }

    @GetMapping("/keywords")
    @Operation(summary = "로그인한 사용자의 희망 키워드 조회", description = "인증된 사용자가 선택한 희망 키워드 목록을 조회합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "키워드 조회 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<List<UserKeywordDto>>> getUserKeywords(@AuthenticationPrincipal User user) {
        List<UserKeywordDto> userKeywords = userService.getUserKeywords(user);
        return ResponseEntity.ok(ApiResponse.onSuccess(userKeywords));
    }

    @PatchMapping("/investment-tendency")
    @Operation(summary = "사용자 자금 운용 성향 수정", description = "로그인한 사용자의 자금 운용 성향을 수정합니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "투자 성향 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 유효성 검사 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<String>> updateInvestmentTendancy(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody InvestmentTendencyUpdateRequestDto requestDto) {
        userService.updateInvestmentTendancy(user, requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess("자금 운용 성향이 성공적으로 업데이트되었습니다."));
    }

    @PutMapping("/keywords")
    @Operation(summary = "사용자 희망 키워드 수정", description = "로그인한 사용자의 희망 키워드 목록을 수정합니다. 기존 키워드는 삭제되고 새로운 키워드로 대체됩니다.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "희망 키워드 수정 성공"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (입력값 유효성 검사 실패)", content = @Content(schema = @Schema(implementation = ErrorResponse.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증 실패", content = @Content(schema = @Schema(implementation = ErrorResponse.class)))
    })
    public ResponseEntity<ApiResponse<String>> updateUserKeywords(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody UserKeywordsUpdateRequestDto requestDto) {
        userService.updateUserKeywords(user, requestDto);
        return ResponseEntity.ok(ApiResponse.onSuccess("희망 키워드 목록이 성공적으로 업데이트되었습니다."));
    }
}
