package com.know_who_how.main_server.inheritance.repository;

import com.know_who_how.main_server.global.entity.Inheritance.InheritanceRecipient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface InheritanceRecipientRepository extends JpaRepository<InheritanceRecipient, Long> {
    // 토큰(accessLink)으로 수신자 정보 조회 (비회원 접근 검증)
    Optional<InheritanceRecipient> findByAccessLink(String accessLink);

    // 현재 시간보다 예약 시간이 빠르거나 같고, 아직 발송되지 않은 수신자 목록 조회
    @Query("SELECT r FROM InheritanceRecipient r WHERE r.scheduledSendDate <= :now AND r.actualSentDate IS NULL")
    List<InheritanceRecipient> findRecipientsToSend(LocalDateTime now);

}