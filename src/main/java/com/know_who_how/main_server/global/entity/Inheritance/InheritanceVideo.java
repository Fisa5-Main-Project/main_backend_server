package com.know_who_how.main_server.global.entity.Inheritance;

import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inheritance_videos")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class InheritanceVideo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "video_id")
    private Long videoId;

    // Inheritance와의 1:1 관계 (FK: inheritance_id)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inheritance_id", nullable = false, unique = true)
    private Inheritance inheritance;

    // S3에 저장된 파일 경로 (Presigned URL 생성에 사용)
    @Column(name = "s3_object_key", nullable = false)
    private String s3ObjectKey;

    // Recipient와의 1:N 관계
    @OneToMany(mappedBy = "video", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InheritanceRecipient> recipients = new ArrayList<>();

    @Builder
    public InheritanceVideo(Inheritance inheritance, String s3ObjectKey) {
        this.inheritance = inheritance;
        this.s3ObjectKey = s3ObjectKey;
        // 양방향 연관관계 설정
        if (inheritance != null && inheritance.getVideo() != this) {
            inheritance.setVideo(this);
        }
    }

    // InheritanceVideo가 Inheritance 객체를 설정하도록 하는 메서드
    public void setInheritance(Inheritance inheritance) {
        this.inheritance = inheritance;
    }

    // 수신자 추가 메서드
    public void addRecipient(InheritanceRecipient recipient) {
        this.recipients.add(recipient);
        if (recipient.getVideo() != this) {
            recipient.setVideo(this);
        }
    }
}