package com.know_who_how.main_server.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class AuthSignupRequest {

    @NotBlank
    private String loginId;

    @NotBlank
    private String password;

    @NotBlank
    private String name;

    @NotBlank
    private String phoneNum;

    @NotBlank
    private LocalDate birth;

    @NotBlank
    private String gender;

    @NotBlank
    private String job;

    @NotNull
    @Valid
    private List<TermAgreementRequest> termAgreements;

    @NotNull
    @Valid
    private UserKeywordsRequest keywords;
}
