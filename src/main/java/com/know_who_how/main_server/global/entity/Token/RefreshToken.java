package com.know_who_how.main_server.global.entity.Token;

import com.know_who_how.main_server.global.entity.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "refresh_token")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, cascade =  CascadeType.ALL)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "token_value", nullable = false, unique = true, length = 500)
    private String tokenValue;

    @Column(name = "expiry_date", nullable = false)
    private Instant expiryDate;

    public void updateToken(String newTokenValue, Instant newExpiryDate) {
        this.tokenValue = newTokenValue;
        this.expiryDate = newExpiryDate;
    }
}
