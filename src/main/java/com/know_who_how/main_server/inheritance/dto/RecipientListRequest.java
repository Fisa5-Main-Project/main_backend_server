package com.know_who_how.main_server.inheritance.dto;

import java.util.List;

// 수신자 등록 컨트롤러 요청 DTO(수신자 리스트)
public record RecipientListRequest(
        List<RecipientRegistrationRequest> recipients
) {}
