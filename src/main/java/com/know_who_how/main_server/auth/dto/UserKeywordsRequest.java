package com.know_who_how.main_server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public class UserKeywordsRequest {

    @NotEmpty
    private List<String> retirementKeywords;

    @NotBlank
    private String investmentTendency;
}
