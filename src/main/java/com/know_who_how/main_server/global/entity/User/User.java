package com.know_who_how.main_server.global.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Entity
@Table(name="users")
@Getter
@NoArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "login_id", unique = true, nullable = false)
    private String loginId;

    @Column(nullable = false)
    private String password;

    @Column(name = "phone_num", unique = true, nullable = false)
    private String phoneNum;

    // 날짜(YYYY-MM-DD) 형식으로 저장
    @Temporal(TemporalType.DATE)
    @Column(nullable = false)
    private LocalDate birth;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Gender gender;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String job;

    @Column(name = "asset_total")
    private Long assetTotal;

}
