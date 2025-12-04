package com.know_who_how.main_server.admin.service;

import com.know_who_how.main_server.admin.dto.UserResponseDto;
import com.know_who_how.main_server.admin.dto.UserUpdateRequestDto; // UserUpdateRequestDto 임포트
import com.know_who_how.main_server.global.entity.User.User; // User 엔티티 임포트
import com.know_who_how.main_server.global.exception.CustomException; // CustomException 임포트
import com.know_who_how.main_server.global.exception.ErrorCode; // ErrorCode 임포트
import com.know_who_how.main_server.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder; // PasswordEncoder 임포트
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminUserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder; // PasswordEncoder 주입

    public List<UserResponseDto> getUsers() {
        return userRepository.findAll().stream()
                .map(UserResponseDto::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateUser(Long userId, UserUpdateRequestDto requestDto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        // 이름 업데이트
        if (requestDto.getName() != null && !requestDto.getName().isEmpty()) {
            user.updateName(requestDto.getName());
        }

        // 비밀번호 초기화
        if (requestDto.getResetPassword() != null && requestDto.getResetPassword()) {
            String defaultEncodedPassword = passwordEncoder.encode("password123!");
            user.updatePassword(defaultEncodedPassword);
        }

        // 마이데이터 연동 해제
        if (requestDto.getDisconnectMyData() != null && requestDto.getDisconnectMyData()) {
            user.disconnectMydata();
        }

        userRepository.save(user); // 변경사항 저장
    }
}
