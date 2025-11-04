package com.know_who_how.main_server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Getter;

import java.util.List;

@Getter
public class UserKeywordsRequest {

    @NotEmpty
    private List<String> retirementKeywords;

    @NotBlank
    private String investmentTendency;
}
