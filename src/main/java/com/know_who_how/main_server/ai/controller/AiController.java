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
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClientResponseException;
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

@Slf4j
public class AiController {

        private final AiServerService aiServerService;

        @PostMapping("/users/{userId}/vectorize")
        @Operation(summary = "사용자 벡터화", description = "사용자 정보를 AI 서버에 벡터화 요청")
        public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> vectorizeUser(
                        @PathVariable Long userId,
                        @AuthenticationPrincipal User user) {

                log.info("Request to vectorize user: {}", userId);

                // 본인만 벡터화 가능
                if (!user.getUserId().equals(userId)) {
                        log.warn("Unauthorized vectorization attempt by user {} for target user {}", user.getUserId(),
                                        userId);
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(ApiResponse.onFailure("FORBIDDEN", "본인만 벡터화할 수 있습니다.")));
                }

                return aiServerService.vectorizeUser(userId)
                                .map(result -> {
                                        log.info("Successfully vectorized user: {}", userId);
                                        return ResponseEntity.ok(ApiResponse.onSuccess(result));
                                })
                                .onErrorResume(error -> {
                                        log.error("Error during user vectorization for userId: {}", userId, error);
                                        if (error instanceof WebClientResponseException wcre) {
                                                return Mono.just(
                                                                ResponseEntity.status(wcre.getStatusCode())
                                                                                .body(ApiResponse.onFailure(String
                                                                                                .valueOf(wcre.getStatusCode()
                                                                                                                .value()),
                                                                                                wcre.getResponseBodyAsString())));
                                        }
                                        return Mono.just(
                                                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                                        .body(ApiResponse.onFailure("AI_SERVER_ERROR",
                                                                                        error.getMessage())));
                                });
        }

        @GetMapping("/recommendations/{userId}")
        @Operation(summary = "금융상품 추천", description = "AI 기반 맞춤형 금융상품 추천")
        public Mono<ResponseEntity<ApiResponse<RecommendationResponse>>> getRecommendations(
                        @PathVariable Long userId,
                        @AuthenticationPrincipal User user) {

                log.info("Request recommendations for user: {}", userId);

                if (!user.getUserId().equals(userId)) {
                        log.warn("Unauthorized recommendation request by user {} for target user {}", user.getUserId(),
                                        userId);
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(ApiResponse.onFailure("FORBIDDEN", "본인만 조회할 수 있습니다.")));
                }

                return aiServerService.getRecommendations(userId)
                                .map(result -> {
                                        log.info("Successfully retrieved recommendations for user: {}", userId);
                                        return ResponseEntity.ok(ApiResponse.onSuccess(result));
                                })
                                .onErrorResume(error -> {
                                        log.error("Error fetching recommendations for userId: {}", userId, error);
                                        if (error instanceof WebClientResponseException wcre) {
                                                return Mono.just(
                                                                ResponseEntity.status(wcre.getStatusCode())
                                                                                .body(ApiResponse.onFailure(String
                                                                                                .valueOf(wcre.getStatusCode()
                                                                                                                .value()),
                                                                                                wcre.getResponseBodyAsString())));
                                        }
                                        return Mono.just(
                                                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                                        .body(ApiResponse.onFailure("AI_SERVER_ERROR",
                                                                                        error.getMessage())));
                                });
        }

        @PostMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
        @Operation(summary = "챗봇 스트리밍", description = "실시간 AI 챗봇 대화")
        public Flux<ServerSentEvent<String>> streamChat(
                        @RequestBody ChatRequest request,
                        @AuthenticationPrincipal User user) {

                log.info("Request chat stream for user: {}", user.getUserId());

                if (!user.getUserId().equals(request.getUserId())) {
                        log.warn("Unauthorized chat stream attempt by user {} for target user {}", user.getUserId(),
                                        request.getUserId());
                        return Flux.error(new IllegalArgumentException("본인만 사용할 수 있습니다."));
                }

                return aiServerService.streamChat(request)
                                .doOnError(error -> log.error("Error during chat stream for user: {}", user.getUserId(),
                                                error));
        }

        @PostMapping("/chat/feedback")
        @Operation(summary = "챗봇 피드백", description = "추천 상품에 대한 피드백 저장")
        public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> saveFeedback(
                        @RequestBody FeedbackRequest request,
                        @AuthenticationPrincipal User user) {

                log.info("Request to save feedback for user: {}", user.getUserId());

                if (!user.getUserId().equals(request.getUserId())) {
                        log.warn("Unauthorized feedback attempt by user {} for target user {}", user.getUserId(),
                                        request.getUserId());
                        return Mono.just(ResponseEntity.status(HttpStatus.FORBIDDEN)
                                        .body(ApiResponse.onFailure("FORBIDDEN", "본인만 피드백을 남길 수 있습니다.")));
                }

                return aiServerService.saveFeedback(
                                request.getUserId(),
                                request.getSessionId(),
                                request.getMessageId(),
                                request.getFeedback(),
                                request.getProductId())
                                .map(result -> {
                                        log.info("Successfully saved feedback for user: {}", user.getUserId());
                                        return ResponseEntity.ok(ApiResponse.onSuccess(result));
                                })
                                .onErrorResume(error -> {
                                        log.error("Error saving feedback for userId: {}", user.getUserId(), error);
                                        if (error instanceof WebClientResponseException wcre) {
                                                return Mono.just(
                                                                ResponseEntity.status(wcre.getStatusCode())
                                                                                .body(ApiResponse.onFailure(String
                                                                                                .valueOf(wcre.getStatusCode()
                                                                                                                .value()),
                                                                                                wcre.getResponseBodyAsString())));
                                        }
                                        return Mono.just(
                                                        ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                                                        .body(ApiResponse.onFailure("AI_SERVER_ERROR",
                                                                                        error.getMessage())));
                                });
        }
}
