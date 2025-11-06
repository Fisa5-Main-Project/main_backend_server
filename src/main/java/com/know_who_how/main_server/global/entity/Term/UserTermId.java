package com.know_who_how.main_server.global.entity.Term;

import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

// user_id와 term_id 복합키 클래스
@NoArgsConstructor
public class UserTermId implements Serializable {

    private Long user;
    private Long term;

    // UserTermId 객체의 user 필드값 + term 필드 값 모두 같은지 비교
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserTermId userTermId = (UserTermId) o;
        return Objects.equals(user, userTermId.user) && Objects.equals(term, userTermId.term);
    }

    // 두 ID 객체가 같은 분류에 속하는지
    @Override
    public int hashCode() {
        return Objects.hash(user, term);
    }
}
