package com.know_who_how.main_server.ai.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ChatRequest {
    private Long userId;
    private String sessionId;
    private String message;
    private List<Integer> keywords;
}
