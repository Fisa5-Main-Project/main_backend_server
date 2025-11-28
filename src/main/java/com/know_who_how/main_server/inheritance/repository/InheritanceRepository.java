package com.know_who_how.main_server.inheritance.repository;

import com.know_who_how.main_server.global.entity.Inheritance.Inheritance;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InheritanceRepository extends JpaRepository<Inheritance, Long> {
    Optional<Inheritance> findByUserId(Long userId);
}