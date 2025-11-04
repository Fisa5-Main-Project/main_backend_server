package com.know_who_how.main_server.auth.repository;

import com.know_who_how.main_server.global.entity.User.Term;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TermRepository extends JpaRepository<Term,Long> {

    // 필수약관 검증용
    List<Term> findByIsRequired(boolean isRequired);
}
