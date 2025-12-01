package com.know_who_how.main_server.user.repository;

import com.know_who_how.main_server.global.entity.Term.UserTerm;
import com.know_who_how.main_server.global.entity.User.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserTermRepository extends JpaRepository<UserTerm, Long> {
    void deleteAllByUser(User user);
}