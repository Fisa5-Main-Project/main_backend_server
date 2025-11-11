package com.know_who_how.main_server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class UserSignupRequestDto {
    @NotBlank(message = "인증 ID는 필수 입력 항목입니다.")
    private String verificationId;

    @NotNull(message = "약관 동의는 필수입니다.")
    private List<TermAgreementRequest> termAgreements;

    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.")
    private String password;

    @NotBlank(message = "자금운용 성향은 필수 선택 항목입니다.")
    private String financialPropensity;

    @NotNull(message = "키워드는 필수 선택 항목입니다.")
    private List<Long> keywordIds;
}
