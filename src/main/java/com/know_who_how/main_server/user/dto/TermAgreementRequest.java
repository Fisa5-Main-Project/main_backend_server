package com.know_who_how.main_server.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TermAgreementRequest {

    @NotNull
    private Long termId;

    @NotNull
    private Boolean isAgreed;

}
