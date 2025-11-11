package com.know_who_how.main_server.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;

@Getter
public class SmsCertificationRequestDto {
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
}
