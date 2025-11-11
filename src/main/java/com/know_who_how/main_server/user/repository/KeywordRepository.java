package com.know_who_how.main_server.user.repository;

import com.know_who_how.main_server.global.entity.Keyword.Keyword;
import com.know_who_how.main_server.global.entity.Keyword.KeywordType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KeywordRepository extends JpaRepository<Keyword, Long> {
    Optional<Keyword> findById(Long id);
    List<Keyword> findByType(KeywordType type);
}
