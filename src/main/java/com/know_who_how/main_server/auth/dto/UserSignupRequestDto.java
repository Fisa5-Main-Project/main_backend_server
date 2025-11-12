package com.know_who_how.main_server.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class UserSignupRequestDto {
    @Schema(description = "SMS 인증 완료 후 받은 ID", example = "c78a8a1d-2c4f-4f3a-b5f7-5a9a8e3a4b2f")
    @NotBlank(message = "인증 ID는 필수 입력 항목입니다.")
    private String verificationId;

    @Schema(description = "동의한 약관 목록")
    @NotNull(message = "약관 동의는 필수입니다.")
    private List<TermAgreementRequest> termAgreements;

    @Schema(description = "로그인 아이디 (4~20자)", example = "newuser123")
    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해주세요.")
    private String loginId;

    @Schema(description = "비밀번호 (영문, 숫자, 특수문자 포함 8자 이상)", example = "password123!")
    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.")
    private String password;

    @Schema(description = "자금운용 성향", example = "공격투자형")
    @NotBlank(message = "자금운용 성향은 필수 선택 항목입니다.")
    private String financialPropensity;

    @Schema(description = "선택한 은퇴 키워드 ID 목록", example = "[1, 2, 3]")
    @NotNull(message = "키워드는 필수 선택 항목입니다.")
    private List<Long> keywordIds;
}
