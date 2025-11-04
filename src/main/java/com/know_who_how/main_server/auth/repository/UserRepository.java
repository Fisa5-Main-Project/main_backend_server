package com.know_who_how.main_server.auth.repository;

import com.know_who_how.main_server.global.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    // 아이디 중복 확인 API용
    boolean existByLoginId(String loginId);

    // 전화번호 중복 확인 API용
    boolean existsByPhoneNum(String phoneNum);
}
