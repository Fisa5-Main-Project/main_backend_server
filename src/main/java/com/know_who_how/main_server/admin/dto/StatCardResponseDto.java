package com.know_who_how.main_server.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StatCardResponseDto {

    private String title;
    private BigDecimal value;
    private double change; // 변화율 (예: 14.2)
    private ChangeType changeType;
    private String description;

    public enum ChangeType {
        increase,
        decrease,
        neutral
    }
}
