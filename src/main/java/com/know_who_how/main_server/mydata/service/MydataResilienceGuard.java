package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

@Component
public class MydataResilienceGuard {

    private final CircuitBreaker circuitBreaker;
    private final Bulkhead inlineBulkhead;
    private final Bulkhead backgroundBulkhead;

    public MydataResilienceGuard() {
        this(
                createCircuitBreaker(10, 50.0f),
                createBulkhead("mydata-inline", 10),
                createBulkhead("mydata-background", 6)
        );
    }

    private MydataResilienceGuard(
            CircuitBreaker circuitBreaker,
            Bulkhead inlineBulkhead,
            Bulkhead backgroundBulkhead
    ) {
        this.circuitBreaker = circuitBreaker;
        this.inlineBulkhead = inlineBulkhead;
        this.backgroundBulkhead = backgroundBulkhead;
    }

    public <T> T executeInline(Supplier<T> action) {
        return decorate(action, inlineBulkhead).get();
    }

    public <T> T executeBackground(Supplier<T> action) {
        return decorate(action, backgroundBulkhead).get();
    }

    private <T> Supplier<T> decorate(Supplier<T> action, Bulkhead bulkhead) {
        Supplier<T> bulkheadProtected = Bulkhead.decorateSupplier(bulkhead, action);
        return CircuitBreaker.decorateSupplier(circuitBreaker, bulkheadProtected);
    }

    static MydataResilienceGuard forTest(int minimumCalls, float failureRateThreshold) {
        return new MydataResilienceGuard(
                createCircuitBreaker(minimumCalls, failureRateThreshold),
                createBulkhead("mydata-inline-test", 10),
                createBulkhead("mydata-background-test", 6)
        );
    }

    private static CircuitBreaker createCircuitBreaker(int minimumCalls, float failureRateThreshold) {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(Math.max(minimumCalls, 2))
                .minimumNumberOfCalls(minimumCalls)
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .permittedNumberOfCallsInHalfOpenState(2)
                .recordException(MydataResilienceGuard::shouldRecord)
                .build();
        return CircuitBreaker.of("mydata-rs", config);
    }

    private static Bulkhead createBulkhead(String name, int maxConcurrentCalls) {
        return Bulkhead.of(name, BulkheadConfig.custom()
                .maxConcurrentCalls(maxConcurrentCalls)
                .maxWaitDuration(Duration.ZERO)
                .build());
    }

    private static boolean shouldRecord(Throwable error) {
        if (error instanceof CustomException custom) {
            return custom.getErrorCode() == ErrorCode.MYDATA_SERVER_ERROR;
        }
        return true;
    }
}
