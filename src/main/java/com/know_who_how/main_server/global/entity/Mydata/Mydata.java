package com.know_who_how.main_server.global.entity.Mydata;

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
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade =  CascadeType.ALL)
    @MapsId
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "user_refresh_token")
    private String refreshToken;

    @Column(name = "user_scope")
    private String scope;

    // Refresh Token 갱신 메서드
    public void updateRefreshToken(String refreshToken) {
        if (refreshToken != null) {
            this.refreshToken = refreshToken;
        }
    }
}
