package com.know_who_how.main_server.global.entity.Keyword;

import com.know_who_how.main_server.global.entity.User.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "user_keyword")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(UserKeywordId.class)
public class UserKeyword {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "keyword_id")
    private Keyword keyword;

    @Builder
    public UserKeyword(User user, Keyword keyword) {
        this.user = user;
        this.keyword = keyword;
    }
}
