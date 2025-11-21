package com.know_who_how.main_server.global.entity.MyData;

import com.know_who_how.main_server.global.entity.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "MyData")
@Getter
@Builder
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
public class Mydata {

    // users.user_id as PK (1:1 mapping)
    @Id
    @Column(name = "user_id", nullable = false)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_access_token")
    private String accessToken;

    @Column(name = "user_refresh_token")
    private String refreshToken;

    // 토큰 만료(초) 및 Scope
    @Column(name = "user_expires_in")
    private Integer expiresIn;

    @Column(name = "user_scope")
    private String scope;

    public void updateTokens(String accessToken, String refreshToken) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
    }

    // 토큰 메타정보(만료 시간, 스코프) 업데이트
    public void updateTokenMeta(Integer expiresIn, String scope) {
        this.expiresIn = expiresIn;
        this.scope = scope;
    }
}
