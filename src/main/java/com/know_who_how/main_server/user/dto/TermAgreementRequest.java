package com.know_who_how.main_server.user.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TermAgreementRequest {

    @NotBlank
    private Long termId;

    @NotBlank
    private Boolean isAgreed;

}
