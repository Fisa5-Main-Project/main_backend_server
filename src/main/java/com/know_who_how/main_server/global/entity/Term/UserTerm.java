package com.know_who_how.main_server.global.entity.Term;


import com.know_who_how.main_server.global.entity.User.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;

@Entity
@Table(name = "users_term")
@Getter
@Builder
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
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
