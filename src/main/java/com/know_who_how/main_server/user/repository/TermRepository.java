package com.know_who_how.main_server.user.repository;

import com.know_who_how.main_server.global.entity.Term.Term;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TermRepository extends JpaRepository<Term, Long> {

    // 필수약관 검증
    List<Term> findByIsRequired(boolean isRequired);

}
