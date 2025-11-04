package com.know_who_how.main_server.auth.dto;

import jakarta.validation.constraints.NotNull;

public class TermAgreementRequest {

    @NotNull
    private Long termId;

    @NotNull
    private Boolean isAgreed;
}
