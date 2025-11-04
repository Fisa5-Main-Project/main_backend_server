package com.know_who_how.main_server.global.entity.User;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "user_keywords")
@Getter
@Setter
@NoArgsConstructor
public class UserKeyword {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="user_keyword_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id", nullable=false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name="retirement_keywords", nullable = false)
    private RetirementKeyword retirementKeyword;

    @Column(name="investment_tendency", nullable =  false)
    private String investmentTendency;

}
