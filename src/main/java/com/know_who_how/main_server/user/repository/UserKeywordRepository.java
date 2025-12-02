package com.know_who_how.main_server.user.repository;

import com.know_who_how.main_server.global.entity.Keyword.UserKeyword;
import com.know_who_how.main_server.global.entity.Keyword.UserKeywordId;
import com.know_who_how.main_server.global.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface UserKeywordRepository extends JpaRepository<UserKeyword, UserKeywordId> {
    List<UserKeyword> findByUser(User user);
    void deleteAllByUser(User user);
}