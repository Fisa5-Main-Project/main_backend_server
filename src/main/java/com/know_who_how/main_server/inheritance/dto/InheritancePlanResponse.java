package com.know_who_how.main_server.inheritance.dto;

import java.math.BigDecimal;

public record InheritancePlanResponse(
        Long inheritanceId,
        BigDecimal asset,
        String ratio
) {
}
