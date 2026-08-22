package com.know_who_how.main_server.mydata.service;

import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MydataResilienceGuardTest {

    @Test
    @DisplayName("일시적 RS 실패가 임계치를 넘으면 Circuit을 OPEN하고 다음 호출을 즉시 차단한다")
    void executeInline_opensCircuitAfterRepeatedFailures() {
        MydataResilienceGuard guard = MydataResilienceGuard.forTest(2, 50.0f);

        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> guard.executeInline(() -> {
                throw new RuntimeException("RS unavailable");
            })).isInstanceOf(RuntimeException.class);
        }

        assertThatThrownBy(() -> guard.executeInline(() -> "not-called"))
                .isInstanceOf(CallNotPermittedException.class);
    }
}
