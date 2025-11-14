package com.know_who_how.main_server.user.repository;

import com.know_who_how.main_server.global.entity.Token.RefreshToken;
import com.know_who_how.main_server.global.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUser(User user);
    Optional<RefreshToken> findByTokenValue(String tokenValue);
    void deleteByUser(User user);
}
