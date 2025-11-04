package com.know_who_how.main_server.global.entity.User;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "users_term")
@Getter
@Setter
@NoArgsConstructor
@IdClass(UserTermId.class)
public class UserTerm {

    // user_id랑 term_id 복합 키 사용 => @IdClass

    // User 참조 (N:1 관계)
    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "term_id")
    private Term term;

    @Column(name = "is_agreed")
    private Boolean isAgreed;

    @Column(name = "agreed_at")
    private LocalDateTime agreedAt;

}
