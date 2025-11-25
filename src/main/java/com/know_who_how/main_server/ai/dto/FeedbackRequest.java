package com.know_who_how.main_server.ai.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class FeedbackRequest {
    private Long userId;
    private String sessionId;
    private String messageId;
    private String feedback; // "like" or "dislike"
    private String productId;
}
