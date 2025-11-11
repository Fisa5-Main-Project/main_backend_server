package com.know_who_how.main_server.user.dto;

import com.know_who_how.main_server.global.entity.User.Gender;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
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
    @NotNull
    private LocalDate birth;
    @NotNull
    private Gender gender;
    @NotEmpty
    @Valid
    private List<TermAgreementRequest> termAgreements;

}