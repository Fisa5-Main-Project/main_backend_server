package com.know_who_how.main_server.auth.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class TermAgreementRequest {

    @NotNull
    private Long termId;

    @NotNull
    private Boolean isAgreed;
}
