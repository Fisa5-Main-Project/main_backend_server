package com.know_who_how.main_server.auth.repository;

import com.know_who_how.main_server.global.entity.User.UserTerm;
import com.know_who_how.main_server.global.entity.User.UserTermId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserTermRepository extends JpaRepository<UserTerm, UserTermId> {
}
