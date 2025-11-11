package com.know_who_how.main_server.auth;

import com.know_who_how.main_server.auth.dto.*;
import com.know_who_how.main_server.auth.service.SmsCertificationService;
import com.know_who_how.main_server.global.dto.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @PostMapping("/sms-certification/sends")
    public ApiResponse<Void> sendSmsCertification(@Valid @RequestBody SmsCertificationRequestDto requestDto) {
        smsCertificationService.sendSmsCertification(requestDto);
        return ApiResponse.onSuccess();
    }

    /**
     * SMS 인증 번호 확인 요청
     * @param confirmDto SMS 인증 확인 DTO (전화번호, 인증번호)
     * @return 성공 여부
     */
    @PostMapping("/sms-certification/confirms")
    public ApiResponse<Void> confirmSmsCertification(@Valid @RequestBody SmsCertificationConfirmDto confirmDto) {
        smsCertificationService.confirmSmsCertification(confirmDto);
        return ApiResponse.onSuccess();
    }

    /**
     * 회원가입 요청
     * @param requestDto 회원가입 요청 DTO
     * @return 생성된 사용자 ID
     */
    @PostMapping("/signup")
    public ApiResponse<Integer> signup(@Valid @RequestBody UserSignupRequestDto requestDto) {
        Integer userId = authService.signup(requestDto);
        return ApiResponse.onSuccess(userId);
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
     * @param requestDto 아이디 중복 확인 DTO
     * @return 중복 여부 (true: 중복, false: 사용 가능)
     */
    @PostMapping("/check-id")
    public ApiResponse<Boolean> checkLoginIdDuplicate(@Valid @RequestBody CheckIdRequestDto requestDto) {
        boolean isDuplicate = authService.checkLoginIdDuplicate(requestDto);
        return ApiResponse.onSuccess(isDuplicate);
    }
}
