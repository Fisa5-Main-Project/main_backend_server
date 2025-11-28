package com.know_who_how.main_server.global.entity.Inheritance;

import com.know_who_how.main_server.global.entity.User.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "inheritance")
@Getter
@Setter
@NoArgsConstructor
public class Inheritance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inheritance_id")
    private Long inheritanceId;

    // User와의 1:1 관계 (FK: user_id)
    // User 엔티티의 PK를 FK로 사용
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "asset", nullable = false, precision = 20, scale = 2)
    private BigDecimal asset; // 상속 자산

    @Column(name = "ratio", nullable = false)
    private String ratio; // 상속 비율 정보 (VARCHAR)

    // InheritanceVideo와의 1:1 관계
    @OneToOne(mappedBy = "inheritance", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private InheritanceVideo video;

    @Builder
    public Inheritance(User user, BigDecimal asset, String ratio) {
        this.user = user;
        this.asset = asset;
        this.ratio = ratio;
    }

    
    // 상속 계획 업데이트 메서드
    public void updatePlan(BigDecimal asset, String ratio) {
        this.asset = asset;
        this.ratio = ratio;
    }

    // 영상편지 설정 시 호출되는 메서드 - 객체 불일치 방지(1:1 양방향 매핑)
    public void setVideo(InheritanceVideo video) {
        this.video = video;
        if (video != null && video.getInheritance() != this) {
            video.setInheritance(this);
        }
    }
}