package com.know_who_how.main_server.user;

import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.user.dto.UserSignupRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService userService;

    // 아이디 중복 확인용 API
    @GetMapping("/{userId}/exists-id")
    public ResponseEntity<ApiResponse<?>> checkLoginId(
            @RequestParam @NotBlank String userId) {
        userService.checkLoginIdDuplicate(userId);

        return ResponseEntity.ok(ApiResponse.onSuccess());
    }

    // 전화번호 중복 확인용 API
    @GetMapping("/{phoneNume}/exists-phonenum")
    public ResponseEntity<ApiResponse<?>> checkPhoneNum(
            @RequestParam @NotBlank String phoneNum){
        userService.checkPhoneNumDuplicate(phoneNum);

        return  ResponseEntity.ok(ApiResponse.onSuccess());
    }

    // 회원 가입 API
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<?>> signup(
            @Valid @RequestBody UserSignupRequest request){
        userService.registerUser(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.onSuccess());
    }

}
