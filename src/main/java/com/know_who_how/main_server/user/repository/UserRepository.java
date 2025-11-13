package com.know_who_how.main_server.user.repository;

import com.know_who_how.main_server.global.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    // 아이디 중복 확인 API용
    boolean existsByLoginId(String loginId);
    Optional<User> findByLoginId(String loginId); // AuthService에서 사용

    // 전화번호 중복 확인 API용
    boolean existsByPhoneNum(String phoneNum);
    Optional<User> findByPhoneNum(String phoneNum); // AuthService에서 사용

}
