package com.know_who_how.main_server.auth;

import com.know_who_how.main_server.global.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface AuthRepository extends JpaRepository<User, Integer> {


    Optional<User> findByLoginId(String loginId);

    /**
     * loginId 중복 검사용
     */
    boolean existsByLoginId(String loginId);
}
