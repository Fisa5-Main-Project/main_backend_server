package com.know_who_how.main_server.inheritance.dto;

import java.time.LocalDateTime;

// 수신자 등록 요청 DTO (이메일, 예약 발송 시점) (FE->BE)
public record RecipientRegistrationRequest(
        String email,
        LocalDateTime scheduledSendDate
) {}
