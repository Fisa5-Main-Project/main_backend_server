package com.know_who_how.main_server.global.entity.Keyword;

import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

@NoArgsConstructor
public class UserKeywordId implements Serializable {

    private Integer user;
    private Long keyword;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserKeywordId that = (UserKeywordId) o;
        return Objects.equals(user, that.user) && Objects.equals(keyword, that.keyword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(user, keyword);
    }
}
