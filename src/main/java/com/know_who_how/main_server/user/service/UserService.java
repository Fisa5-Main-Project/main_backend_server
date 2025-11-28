package com.know_who_how.main_server.user.service;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.user.dto.InvestmentTendencyUpdateRequestDto; // 추가
import com.know_who_how.main_server.user.dto.ProfileUpdateRequestDto;
import com.know_who_how.main_server.user.dto.UserKeywordDto; // 추가
import com.know_who_how.main_server.user.dto.UserResponseDto;
import com.know_who_how.main_server.user.repository.UserKeywordRepository; // 추가
import com.know_who_how.main_server.user.repository.UserRepository;
import java.util.List; // 추가
import java.util.stream.Collectors; // 추가
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class UserService {

    private final UserRepository userRepository;
    private final UserKeywordRepository userKeywordRepository; // 추가

    public UserResponseDto getUserInfo(User user) {
        return UserResponseDto.from(user);
    }

    @Transactional
    public void updateProfile(User user, ProfileUpdateRequestDto requestDto) {
        User foundUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        foundUser.updatePhoneNum(requestDto.getPhoneNum());
    }

    public List<UserKeywordDto> getUserKeywords(User user) { // 추가
        return userKeywordRepository.findByUser(user).stream()
                .map(userKeyword -> UserKeywordDto.from(userKeyword.getKeyword()))
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateInvestmentTendancy(User user, InvestmentTendencyUpdateRequestDto requestDto) { // 추가
        User foundUser = userRepository.findById(user.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        foundUser.updateInvestmentTendancy(requestDto.getInvestmentTendancy());
    }
}