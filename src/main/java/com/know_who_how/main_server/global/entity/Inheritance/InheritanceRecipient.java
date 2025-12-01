package com.know_who_how.main_server.global.entity.Inheritance;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "inheritance_recipients")
@Getter
@NoArgsConstructor
public class InheritanceRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipient_id")
    private Long recipientId;

    // InheritanceVideo와의 N:1 관계 (FK: video_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "video_id", nullable = false)
    private InheritanceVideo video;

    @Column(nullable = false)
    private String email;

    // 사용자가 입력한 발송 예약 시점
    @Column(name = "scheduled_send_date", nullable = false)
    private LocalDateTime scheduledSendDate;

    // 시스템이 실제로 메일을 발송 완료한 시점 (초기값 null)
    @Column(name = "actual_sent_date")
    private LocalDateTime actualSentDate;

    // 비회원 접근용 고유 링크 (UUID, JWT 등을 사용하여 암호화된 토큰을 저장)
    // 이 링크는 DB에 저장하고, 실제 사용자에게는 이 값을 포함한 전체 URL 전송
    @Column(name = "access_link", nullable = false, unique = true)
    private String accessLink;

    @Builder
    public InheritanceRecipient(InheritanceVideo video, String email, String accessLink, LocalDateTime scheduledSendDate) {
        this.video = video;
        this.email = email;
        this.accessLink = accessLink;
        this.scheduledSendDate = scheduledSendDate;
        this.actualSentDate = null; // 초기에는 발송되지 않음
    }

    // InheritanceVideo 객체를 설정하도록 하는 메서드 (양방향 매핑용)
    public void setVideo(InheritanceVideo video) {
        this.video = video;
    }

    // 이메일 발송 성공 시 호출되어 실제 발송 시간 기록
    public void markSent(LocalDateTime actualSentDate) {
        this.actualSentDate = actualSentDate;
    }

    // 토큰 무효화
    public void invalidateLink(){
        this.accessLink = null;
    }
}