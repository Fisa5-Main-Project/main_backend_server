package com.know_who_how.main_server.user.service;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.user.dto.ProfileUpdateRequestDto;
import com.know_who_how.main_server.user.dto.UserKeywordDto; // UserKeywordDto 임포트 추가
import com.know_who_how.main_server.user.dto.UserResponseDto;
import com.know_who_how.main_server.user.repository.UserKeywordRepository; // UserKeywordRepository 임포트 추가
import com.know_who_how.main_server.user.repository.UserRepository;
import java.util.List; // List 임포트 추가
import java.util.stream.Collectors; // Collectors 임포트 추가
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserKeywordRepository userKeywordRepository; // UserKeywordRepository 주입

    public UserResponseDto getUserInfo(User user) {
        return UserResponseDto.from(user);
    }

    @Transactional
    public void updateProfile(User user, ProfileUpdateRequestDto requestDto) {
        // 현재 로그인된 사용자의 ID로 DB에서 User 엔티티를 다시 로드 (영속성 컨텍스트 관리)
        User foundUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        foundUser.updatePhoneNum(requestDto.getPhoneNum());
        // save를 명시적으로 호출하지 않아도 @Transactional에 의해 더티체킹으로 자동 업데이트됨
    }

    public List<UserKeywordDto> getUserKeywords(User user) {
        return userKeywordRepository.findByUser(user).stream()
                .map(userKeyword -> UserKeywordDto.from(userKeyword.getKeyword()))
                .collect(Collectors.toList());
    }
}
