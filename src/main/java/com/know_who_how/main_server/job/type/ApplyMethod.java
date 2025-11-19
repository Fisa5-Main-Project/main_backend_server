package com.know_who_how.main_server.job.type;

import lombok.Getter;

import java.util.Arrays;

public enum ApplyMethod {
    ONLINE("CM0801", "온라인"),
    EMAIL("CM0802", "이메일"),
    FAX("CM0803", "팩스"),
    VISIT("CM0804", "방문"),
    UNKNOWN(null, "정보 없음"); // 기본값 또는 알 수 없는 코드 처리

    private final String code;
    @Getter
    private final String displayName;

    ApplyMethod(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 코드 문자열을 Enum 값으로 변환.
     * 일치하는 코드가 없으면 UNKNOWN 반환
     */
    public static ApplyMethod fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        return Arrays.stream(values())
                .filter(method -> code.equals(method.code))
                .findFirst()
                .orElse(UNKNOWN);
    }

}