package com.know_who_how.main_server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;

import java.util.List;

@Getter
public class UserSignupRequestDto {
    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    private String name;

    @NotBlank(message = "생년월일은 필수 입력 항목입니다.")
    @Pattern(regexp = "^\\d{8}$", message = "생년월일은 YYYYMMDD 형식의 8자리 숫자여야 합니다.")
    private String birthDate;

    @NotNull(message = "주민등록번호 뒷자리 첫 번째 숫자는 필수 입력 항목입니다.")
    private Integer genderDigit;

    @NotBlank(message = "통신사는 필수 입력 항목입니다.")
    private String telecom;

    @NotBlank(message = "전화번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "유효하지 않은 전화번호 형식입니다.")
    private String phoneNum;

    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자 이상 20자 이하로 입력해주세요.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 8자 이상이며, 영문, 숫자, 특수문자를 포함해야 합니다.")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 입력 항목입니다.")
    private String passwordConfirm;

    @NotNull(message = "약관 동의는 필수입니다.")
    private List<Long> agreedTermIds;

    @NotNull(message = "자금운용 성향 키워드는 필수 선택 항목입니다.")
    private Long investmentKeywordId;

    private List<Long> retirementKeywordIds; // 은퇴 후 희망 키워드는 선택 사항일 수 있음
}
