package com.know_who_how.main_server.ai.controller;

import com.know_who_how.main_server.ai.dto.ChatRequest;
import com.know_who_how.main_server.ai.dto.FeedbackRequest;
import com.know_who_how.main_server.ai.dto.RecommendationResponse;
import com.know_who_how.main_server.ai.service.AiServerService;
import com.know_who_how.main_server.global.dto.ApiResponse;
import com.know_who_how.main_server.global.entity.User.User;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AI 서버 프록시 컨트롤러
 */
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
@Tag(name = "5. AI 추천 API")
public class AiController {

    private final AiServerService aiServerService;

    @PostMapping("/users/{userId}/vectorize")
    @Operation(summary = "사용자 벡터화", description = "사용자 정보를 AI 서버에 벡터화 요청")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> vectorizeUser(
            @PathVariable Long userId,
            @AuthenticationPrincipal User user) {

        // 본인만 벡터화 가능
        if (!user.getUserId().equals(userId)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.onFailure("FORBIDDEN", "본인만 벡터화할 수 있습니다.")));
        }

        return aiServerService.vectorizeUser(userId)
                .map(result -> ResponseEntity.ok(ApiResponse.onSuccess(result)))
                .onErrorResume(error -> Mono.just(
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.onFailure("AI_SERVER_ERROR", error.getMessage()))));
    }

    @GetMapping("/recommendations/{userId}")
    @Operation(summary = "금융상품 추천", description = "AI 기반 맞춤형 금융상품 추천")
    public Mono<ResponseEntity<ApiResponse<RecommendationResponse>>> getRecommendations(
            @PathVariable Long userId,
            @AuthenticationPrincipal User user) {

        if (!user.getUserId().equals(userId)) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.onFailure("FORBIDDEN", "본인만 조회할 수 있습니다.")));
        }

        return aiServerService.getRecommendations(userId)
                .map(result -> ResponseEntity.ok(ApiResponse.onSuccess(result)))
                .onErrorResume(error -> Mono.just(
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.onFailure("AI_SERVER_ERROR", error.getMessage()))));
    }

    @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "챗봇 스트리밍", description = "실시간 AI 챗봇 대화")
    public Flux<ServerSentEvent<String>> streamChat(
            @RequestBody ChatRequest request,
            @AuthenticationPrincipal User user) {

        if (!user.getUserId().equals(request.getUserId())) {
            return Flux.error(new IllegalArgumentException("본인만 사용할 수 있습니다."));
        }

        return aiServerService.streamChat(request);
    }

    @PostMapping("/chat/feedback")
    @Operation(summary = "챗봇 피드백", description = "추천 상품에 대한 피드백 저장")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> saveFeedback(
            @RequestBody FeedbackRequest request,
            @AuthenticationPrincipal User user) {

        if (!user.getUserId().equals(request.getUserId())) {
            return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(ApiResponse.onFailure("FORBIDDEN", "본인만 피드백을 남길 수 있습니다.")));
        }

        return aiServerService.saveFeedback(
                request.getUserId(),
                request.getSessionId(),
                request.getMessageId(),
                request.getFeedback(),
                request.getProductId())
                .map(result -> ResponseEntity.ok(ApiResponse.onSuccess(result)))
                .onErrorResume(error -> Mono.just(
                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                .body(ApiResponse.onFailure("AI_SERVER_ERROR", error.getMessage()))));
    }
}
