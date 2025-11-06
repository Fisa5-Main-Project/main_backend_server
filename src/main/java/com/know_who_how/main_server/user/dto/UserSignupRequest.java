package com.know_who_how.main_server.user.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
public class UserSignupRequest {
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
    private Gender gender;
    @NotBlank
    private String job;

    @NotBlank
    @Valid
    private List<TermAgreementRequest> termAgreements;

}
