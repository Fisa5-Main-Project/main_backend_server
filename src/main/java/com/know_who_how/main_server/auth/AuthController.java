package com.know_who_how.main_server.auth;

import com.know_who_how.main_server.auth.dto.*;
import com.know_who_how.main_server.auth.service.SmsCertificationService;
import com.know_who_how.main_server.global.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SmsCertificationService smsCertificationService;

    /**
     * SMS 인증 번호 전송 요청
     * @param requestDto SMS 인증 요청 DTO (이름, 생년월일, 성별, 통신사, 전화번호)
     * @return 성공 여부
     */
    @PostMapping("/signup/send-sms")
    public ApiResponse<Map<String, String>> sendSmsCertification(@Valid @RequestBody SmsCertificationRequestDto requestDto) {
        String verificationId = smsCertificationService.sendSmsCertification(requestDto);
        return ApiResponse.onSuccess(Map.of("verificationId", verificationId));
    }

    /**
     * SMS 인증 번호 확인 요청
     * @param confirmDto SMS 인증 확인 DTO (전화번호, 인증번호)
     * @return 성공 여부
     */
    @PostMapping("/signup/check-code")
    public ApiResponse<String> confirmSmsCertification(@Valid @RequestBody SmsCertificationConfirmDto confirmDto) {
        String message = smsCertificationService.confirmSmsCertification(confirmDto);
        return ApiResponse.onSuccess(message);
    }

    /**
     * 회원가입 요청
     * @param requestDto 회원가입 요청 DTO
     * @return 생성된 사용자 ID
     */
    @PostMapping("/signup/submit")
    public ApiResponse<String> signup(@Valid @RequestBody UserSignupRequestDto requestDto) {
        authService.signup(requestDto);
        return ApiResponse.onSuccess("회원가입이 완료되었습니다.");
    }

    /**
     * 로그인 요청
     * @param requestDto 로그인 요청 DTO
     * @return JWT 토큰 (AccessToken, RefreshToken)
     */
    @PostMapping("/login")
    public ApiResponse<TokenResponseDto> login(@Valid @RequestBody LoginRequestDto requestDto) {
        TokenResponseDto token = authService.login(requestDto);
        return ApiResponse.onSuccess(token);
    }

    /**
     * 아이디 중복 확인 요청
     * @param loginId 중복 확인 DTO
     * @return 중복 여부 (true: 중복, false: 사용 가능)
     */
    @GetMapping("/signup/check-login-id")
    public ApiResponse<String> checkLoginIdDuplicate(@RequestParam @NotBlank(message = "아이디는 필수 입력 항목입니다.") @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해주세요.") String loginId) {
        String message = authService.checkLoginIdDuplicate(loginId);
        return ApiResponse.onSuccess(message);
    }

    @PostMapping("/logout")
    public ApiResponse<String> logout(@RequestHeader("Authorization") String refreshToken) {
        authService.logout(refreshToken);
        return ApiResponse.onSuccess("로그아웃이 완료되었습니다.");
    }
}
