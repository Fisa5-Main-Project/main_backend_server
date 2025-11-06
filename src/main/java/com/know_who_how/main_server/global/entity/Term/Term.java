package com.know_who_how.main_server.global.entity.Term;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name="term")
@Getter
@NoArgsConstructor
public class Term {
    @Id
    @GeneratedValue(strategy=GenerationType.IDENTITY)
    @Column(name = "term_id")
    private Long id;

    @Column(name="term_name", nullable = false)
    private String termName;

    @Column(name ="is_required", nullable = false)
    private boolean isRequired;
}
