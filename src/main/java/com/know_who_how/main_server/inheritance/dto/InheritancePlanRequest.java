package com.know_who_how.main_server.inheritance.dto;

import java.math.BigDecimal;

// 상속 기록 (자산, 비율)을 요청, 응답하는 DTO
public record InheritancePlanRequest (
    BigDecimal asset,
    String ratio
){}
