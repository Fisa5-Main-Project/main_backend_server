package com.know_who_how.main_server.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class UserKeywordsUpdateRequestDto {
    @Schema(description = "선택된 키워드 ID 목록", example = "[1, 6, 15]")
    @NotNull(message = "키워드 ID 목록은 필수입니다.")
    @Size(min = 1, max = 5, message = "최소 1개, 최대 5개의 키워드를 선택해야 합니다.")
    private List<Long> keywordIds;
}