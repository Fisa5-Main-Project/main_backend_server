package com.know_who_how.main_server.inheritance.repository;

import com.know_who_how.main_server.global.entity.Inheritance.InheritanceRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface InheritanceRecipientRepository extends JpaRepository<InheritanceRecipient, Long> {
    // 토큰(accessLink)으로 수신자 정보 조회 (비회원 접근 검증)
    Optional<InheritanceRecipient> findByAccessLink(String accessLink);
}