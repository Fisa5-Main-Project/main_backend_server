package com.know_who_how.main_server.user;

import com.know_who_how.main_server.user.dto.UserSignupRequest;

public interface UserService {

    /**
     * 로그인 ID 중복검사
     * @param loginId 검사할 로그인 ID
     */
    void checkLoginIdDuplicate(String loginId);

    /**
     * 전화번호 중복 검사
     * @param phoneNum 검사할 전화번호
     */
    void checkPhoneNumDuplicate(String phoneNum);

    /**
     * 회원가입 요청 처리
     * @param request 회원가입 요청 DTO
     */
    void registerUser(UserSignupRequest request);

}
