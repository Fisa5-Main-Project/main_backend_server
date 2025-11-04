package com.know_who_how.main_server.auth;

import com.know_who_how.main_server.auth.dto.AuthSignupRequest;
import com.know_who_how.main_server.global.dto.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthController {

    private final UserService userService;

    // 아이디 중복 확인용 API
    @GetMapping("/check-loginid")
    public ResponseEntity<ApiResponse<?>> checkLoginId(
            @RequestParam @NotBlank String loginId) {
        userService.checkLoginIdDuplicate(loginId);

        return ResponseEntity.ok(ApiResponse.onSuccess());
    }
    
    // 전화번호 중복 확인용 API
    @GetMapping("/check-phonenum")
    public ResponseEntity<ApiResponse<?>> checkPhoneNum(
            @RequestParam @NotBlank String phoneNum){
        userService.checkPhoneNumDuplicate(phoneNum);

        return  ResponseEntity.ok(ApiResponse.onSuccess());
    }

    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<?>> signup(
            @Valid @RequestBody AuthSignupRequest request){
        userService.registerUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess());
    }

}
