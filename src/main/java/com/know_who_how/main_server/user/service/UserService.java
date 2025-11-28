package com.know_who_how.main_server.user.service;

import com.know_who_how.main_server.global.entity.Keyword.Keyword;
import com.know_who_how.main_server.global.entity.Keyword.UserKeyword;
import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.user.dto.*;
import com.know_who_how.main_server.user.repository.KeywordRepository;
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
    private final UserKeywordRepository userKeywordRepository;
    private final KeywordRepository keywordRepository;

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

    @Transactional
    public void updateUserKeywords(User user, UserKeywordsUpdateRequestDto requestDto) {
        User foundUser = userRepository.findById(user.getUserId()).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        // 1. 해당 사용자의 기존 UserKeyword 연관 관계를 모두 삭제
        userKeywordRepository.deleteAllByUser(foundUser);
        // 2. 새로운 Keyword 엔티티 목록을 ID로 조회
        List<Keyword> keywords = keywordRepository.findAllById(requestDto.getKeywordIds());
        // 3. 요청된 ID 개수와 실제 조회된 엔티티 개수가 다르면, 유효하지 않은 ID가 포함된 것이므로 예외 처리
        if (keywords.size() != requestDto.getKeywordIds().size()) {
            throw new IllegalArgumentException("존재하지 않는 키워드 ID가 포함되어 있습니다.");
        }
        // 4. 새로운 UserKeyword 연관 관계 생성
        List<UserKeyword> newUserKeywords = keywords.stream()
                .map(keyword -> UserKeyword.builder()
                        .user(foundUser)
                        .keyword(keyword)
                        .build())
                .collect(Collectors.toList());
        // 5. 새로운 연관 관계 저장
        userKeywordRepository.saveAll(newUserKeywords);
    }
}