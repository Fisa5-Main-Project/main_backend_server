package com.know_who_how.main_server.job.type;

import lombok.Getter;

import java.util.Arrays;

public enum EmploymentType {
    FULL_TIME("CM0101", "정규직"),
    CONTRACT("CM0102", "계약직"),
    PART_TIME("CM0103", "시간제일자리"),
    DAILY("CM0104", "일당직"),
    ETC("CM0105", "기타"),
    UNKNOWN(null, "정보 없음"); // 기본값 처리

    private final String code;
    @Getter
    private final String displayName;

    EmploymentType(String code, String displayName) {
        this.code = code;
        this.displayName = displayName;
    }

    /**
     * 코드 문자열을 Enum 값으로 변환
     * 일치하는 코드가 없으면 UNKNOWN 반환
     */
    public static EmploymentType fromCode(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        // Open API 리스트 응답에서 코드 대신 이름(예: '정규직')이 올 수 있으므로 이름으로도 매핑 시도
        return Arrays.stream(values())
                .filter(type -> code.equals(type.code) || code.equals(type.displayName))
                .findFirst()
                .orElse(UNKNOWN);
    }

}