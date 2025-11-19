package com.know_who_how.main_server.job.client;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.job.dto.external.ExternalApiResponse;
import com.know_who_how.main_server.job.dto.external.ExternalJobDetailItemWrapper;
import com.know_who_how.main_server.job.dto.external.ExternalJobListItems;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.codec.json.Jackson2JsonDecoder;
import org.springframework.http.codec.json.Jackson2JsonEncoder;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.util.retry.Retry;

import java.time.Duration;


// Web Client를 사용해 외부 Open API를 실제로 호출하는 Client
@Slf4j
@Component
public class JobOpenApiClient {

    private final WebClient webClient; // 비동기 HTTP 클라이언트
    private final String serviceKey; // application.yml에서 주입받는 키

    // WebClient.Builder를 주입받아 생성자에서 초기화
    public JobOpenApiClient(
            WebClient.Builder webClientBuilder,
            @Value("${open-api.base-url}") String baseUrl,
            @Value("${open-api.service-key}") String serviceKey
    ) {

        // Open API 응답으로 빈 문자열이 올 경우 null로 처리하기 위함.
        ObjectMapper objectMapper = new ObjectMapper();

        // 빈 문자열("")을 null 객체로 받아들임.
        objectMapper.configure(DeserializationFeature.ACCEPT_EMPTY_STRING_AS_NULL_OBJECT, true);
        // "item"이 단일 객체로 와도 배열로 처리하도록 허용
        objectMapper.configure(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY, true);


        // 빈 문자열/단일 객체 등을 처리하도록 커스텀한 ObjectMapper를 WebClient에 설정
        ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(configurer -> {
                    configurer.defaultCodecs().configureDefaultCodec(codec -> {
                                // 4. 그 중에서 Jackson (JSON) 디코더만 찾아서
                                if (codec instanceof Jackson2JsonDecoder) {
                                    // 5. ObjectMapper를 우리 커스텀으로 교체
                                    ((Jackson2JsonDecoder) codec).setObjectMapper(objectMapper);
                                }
                                // 6. JSON 인코더도 동일하게 교체
                                if (codec instanceof Jackson2JsonEncoder) {
                                    ((Jackson2JsonEncoder) codec).setObjectMapper(objectMapper);
                                }
                            }
                    );
                }).build();

        // webClient 인스턴스 생성
        this.webClient = webClientBuilder.baseUrl(baseUrl).exchangeStrategies(strategies).build();
        this.serviceKey = serviceKey;
    }

    /**
     * 1. 채용 공고 리스트 조회 (getJobList)
     * @param search
     */
    public ExternalApiResponse<ExternalJobListItems> fetchJobs(String search, String empType, int page, int size) {
        // .get()부터 .block()까지가 하나의 비동기 요청 파이프라인
        return webClient.get()// 1. HTTP GET요청 시작
                .uri(uriBuilder -> uriBuilder //2. URL 구성
                        .path("/SenuriService/getJobList")
                        .queryParam("serviceKey", serviceKey) //2-1. 쿼리파라미터
                        .queryParam("pageNo", page)
                        .queryParam("numOfRows", size)
                        .queryParam("search", search)
                        .queryParam("emplymShp", empType)
                        .queryParam("type", "json") // 응답 형식 json으로 고정
                        .build()
                )
                .accept(MediaType.APPLICATION_JSON) // 3. HTTP 'Accept' 헤더를 '/application/json'으로 설정
                .retrieve() // 4. 실제 요청 실행, 응답 처리 준비

                // [에러 처리 1] 클라이언트 에러의 경우
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Open API 4xx Error: status = {}, body={}", clientResponse.statusCode(), errorBody);
                            ErrorCode errorCode;
                            switch(clientResponse.statusCode().value()){
                                case 401:
                                    errorCode = ErrorCode.EXTERNAL_API_UNAUTHORIZED;
                                    break;
                                case 403:
                                    errorCode = ErrorCode.EXTERNAL_API_FORBIDDEN;
                                    break;
                                case 429:
                                    errorCode = ErrorCode.EXTERNAL_API_RATE_LIMIT;
                                    break;
                                default:
                                    errorCode = ErrorCode.EXTERNAL_API_NOT_FOUND;
                            }
                            return Mono.error(new CustomException(errorCode));
                        })
                )

                // [에러 처리 2] 서버 에러의 경우
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Open API 5xx Error: {}", errorBody);
                            return Mono.error(new CustomException(ErrorCode.EXTERNAL_API_SERVER_ERROR));
                        })
                )

                // [성공 처리] 정상 응답(2xx)이 온 경우, DTO에 맞게 변환
                .bodyToMono(ExternalApiResponse.getTypeReferenceForList())

                // [재시도] 예외 발생한 경우 재시도
                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1)) // 1초 간격으로 최대 3회 재시도
                        // 모든 에러가 아닌 우리가 정의한 에러(네트워크/서버 오류)의 경우에만 재시도
                        .filter(throwable -> throwable instanceof CustomException &&
                                ((CustomException) throwable).getErrorCode() == ErrorCode.EXTERNAL_API_SERVER_ERROR)
                )
                // [재시도도 실패한 경우]
                .doOnError(e -> log.error("Open API 'getJobList' 호출 실패", e))

                // 15초간 대기 후 결과 반환
                .block(Duration.ofSeconds(15));
    }

    /**
     * 2. 채용 공고 상세 조회 (getJobInfo)
     */
    public ExternalApiResponse<ExternalJobDetailItemWrapper> fetchJobDetail(String jobId) {
        return webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/SenuriService/getJobInfo")
                        .queryParam("serviceKey", serviceKey)
                        .queryParam("id", jobId)
                        .queryParam("type", "json")
                        .build()
                )
                .accept(MediaType.APPLICATION_JSON)
                .retrieve()
                .onStatus(HttpStatusCode::is4xxClientError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Open API 4xx Error: {}", errorBody);
                            return Mono.error(new CustomException(ErrorCode.EXTERNAL_API_NOT_FOUND));
                        })
                )
                .onStatus(HttpStatusCode::is5xxServerError, clientResponse ->
                        clientResponse.bodyToMono(String.class).flatMap(errorBody -> {
                            log.error("Open API 5xx Error: {}", errorBody);
                            return Mono.error(new CustomException(ErrorCode.EXTERNAL_API_SERVER_ERROR));
                        })
                )
                .bodyToMono(ExternalApiResponse.getTypeReferenceForDetail())

                .retryWhen(Retry.backoff(3, Duration.ofSeconds(1))
                        .filter(throwable -> throwable instanceof CustomException &&
                                ((CustomException) throwable).getErrorCode() == ErrorCode.EXTERNAL_API_SERVER_ERROR)) // 4xx 클라이언트 오류는 재시도해도 성공할 가능성이 낮으므로, 재시도 대상에서 제외
                .doOnError(e -> log.error("Open API 'getJobInfo' 호출 실패", e))
                .block(Duration.ofSeconds(15));
    }

}