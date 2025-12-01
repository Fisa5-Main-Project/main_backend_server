package com.know_who_how.main_server.user.dto;

import com.know_who_how.main_server.global.entity.Keyword.Keyword;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserKeywordDto {
    @Schema(description = "키워드 ID", example = "1")
    private Long id;

    @Schema(description = "키워드 이름", example = "여행")
    private String name;

    public static UserKeywordDto from(Keyword keyword) {
        return UserKeywordDto.builder()
                .id(keyword.getId())
                .name(keyword.getName())
                .build();
    }

}
