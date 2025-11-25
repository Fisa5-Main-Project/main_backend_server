package com.know_who_how.main_server.ai.service;

import com.know_who_how.main_server.ai.dto.ChatRequest;
import com.know_who_how.main_server.ai.dto.RecommendationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Map;

/**
 * AI 서버와 통신하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AiServerService {

        private final WebClient aiWebClient;

        /**
         * 사용자 벡터화 요청
         *
         * @param userId 사용자 ID
         * @return 벡터화 결과
         */
        public Mono<Map<String, Object>> vectorizeUser(Long userId) {
                return aiWebClient.post()
                                .uri("/api/v1/users/{userId}/vectorize", userId)
                                .retrieve()
                                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                                })
                                .doOnSuccess(response -> log.debug("Vectorization successful for userId: {}", userId))
                                .doOnError(error -> log.error("사용자 벡터화 실패: userId={}", userId, error));
        }

        /**
         * 금융상품 추천 조회
         *
         * @param userId 사용자 ID
         * @return 추천 결과
         */
        public Mono<RecommendationResponse> getRecommendations(Long userId) {
                return aiWebClient.get()
                                .uri("/api/v1/recommendations/{userId}", userId)
                                .retrieve()
                                .bodyToMono(RecommendationResponse.class)
                                .doOnSuccess(response -> log.debug("Recommendations retrieved for userId: {}", userId))
                                .doOnError(error -> log.error("추천 조회 실패: userId={}", userId, error));
        }

        /**
         * 챗봇 스트리밍 (SSE)
         *
         * @param request 챗봇 요청
         * @return SSE 스트림
         */
        public Flux<ServerSentEvent<String>> streamChat(ChatRequest request) {
                return aiWebClient.post()
                                .uri("/api/v1/chat/stream")
                                .bodyValue(request)
                                .retrieve()
                                .bodyToFlux(String.class)
                                .map(data -> ServerSentEvent.<String>builder()
                                                .data(data)
                                                .build())
                                .doOnSubscribe(subscription -> log.debug("Chat stream started for user: {}",
                                                request.getUserId()))
                                .doOnError(error -> log.error("챗봇 스트리밍 실패: {}", request, error));
        }

        /**
         * 피드백 저장
         *
         * @param userId    사용자 ID
         * @param sessionId 세션 ID
         * @param messageId 메시지 ID
         * @param feedback  피드백 ("like" or "dislike")
         * @param productId 상품 ID
         * @return 저장 결과
         */
        public Mono<Map<String, Object>> saveFeedback(Long userId, String sessionId,
                        String messageId, String feedback, String productId) {
                Map<String, Object> body = Map.of(
                                "user_id", userId,
                                "session_id", sessionId,
                                "message_id", messageId,
                                "feedback", feedback,
                                "product_id", productId != null ? productId : "");

                return aiWebClient.post()
                                .uri("/api/v1/chat/feedback")
                                .bodyValue(body)
                                .retrieve()
                                .bodyToMono(new ParameterizedTypeReference<Map<String, Object>>() {
                                })
                                .doOnSuccess(response -> log.debug("Feedback saved for userId: {}", userId))
                                .doOnError(error -> log.error("피드백 저장 실패: userId={}", userId, error));
        }
}
