package com.know_who_how.main_server.global.entity.Inheritance;

import com.know_who_how.main_server.global.entity.User.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "inheritance")
@Getter @Setter
@NoArgsConstructor
public class Inheritance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer inheritanceId; // PK

    // ManyToOne: User <- Inheritance
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false) // FK
    private User user;

    @Column(nullable = false)
    private Long asset; // 상속 자산

    @Column(name = "ratio", nullable = false)
    private String ratio; // 비율 정보

    // OneToMany: Inheritance -> InheritanceVideo
    @OneToMany(mappedBy = "inheritance", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InheritanceVideo> videos = new ArrayList<>();
}