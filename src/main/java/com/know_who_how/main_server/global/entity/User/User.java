package com.know_who_how.main_server.global.entity.User;

import jakarta.persistence.*;
import java.util.Date;

@Entity
@Table(name="users")
public class User {

    @Id
    @GeneratedValue
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
    private Date birth;

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
