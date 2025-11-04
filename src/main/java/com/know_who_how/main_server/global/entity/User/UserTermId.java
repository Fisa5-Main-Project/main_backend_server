package com.know_who_how.main_server.global.entity.User;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;

// user_id와 term_id 복합키 클래스
@Getter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserTermId implements Serializable {
    private Long user;
    private Long term;
}
