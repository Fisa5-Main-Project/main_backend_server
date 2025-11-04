package com.know_who_how.main_server.auth.repository;

import com.know_who_how.main_server.global.entity.User.UserKeyword;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserKeywordRepository extends JpaRepository<UserKeyword, Long> {
}
