package com.know_who_how.main_server.mydata.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.mydata.dto.MydataDto;
import com.know_who_how.main_server.mydata.entity.MydataSnapshot;
import com.know_who_how.main_server.mydata.repository.MydataSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MydataSnapshotServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-22T06:00:00Z");

    @Mock
    private MydataSnapshotRepository snapshotRepository;

    private ObjectMapper objectMapper;
    private MydataSnapshotService snapshotService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
        snapshotService = new MydataSnapshotService(
                snapshotRepository,
                objectMapper,
                clock,
                Duration.ofHours(1)
        );
    }

    @Test
    @DisplayName("RS 응답을 기존 MydataDto JSON 형태로 UPSERT한다")
    void upsert_serializesMydataDto() {
        MydataDto dto = new MydataDto();
        dto.setAssets(Collections.emptyList());
        dto.setLiabilities(Collections.emptyList());
        LocalDateTime fetchedAt = LocalDateTime.ofInstant(NOW.minusSeconds(10), ZoneOffset.UTC);

        snapshotService.upsert(1L, dto, fetchedAt);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(snapshotRepository).upsert(
                eq(1L),
                payloadCaptor.capture(),
                eq(1),
                eq(fetchedAt),
                eq(LocalDateTime.ofInstant(NOW, ZoneOffset.UTC))
        );
        assertThat(payloadCaptor.getValue()).contains("\"assets\":[]", "\"liabilities\":[]");
    }

    @Test
    @DisplayName("마지막 동기화가 1시간을 초과했으면 stale Snapshot으로 조회한다")
    void find_returnsStaleSnapshot() {
        MydataSnapshot snapshot = MydataSnapshot.builder()
                .userId(1L)
                .payload("{\"assets\":[],\"liabilities\":[]}")
                .schemaVersion(1)
                .sourceFetchedAt(LocalDateTime.ofInstant(NOW.minus(Duration.ofHours(2)), ZoneOffset.UTC))
                .syncedAt(LocalDateTime.ofInstant(NOW.minus(Duration.ofHours(2)), ZoneOffset.UTC))
                .build();
        when(snapshotRepository.findById(1L)).thenReturn(Optional.of(snapshot));

        var result = snapshotService.find(1L).orElseThrow();

        assertThat(result.stale()).isTrue();
        assertThat(result.data().getAssets()).isEmpty();
        assertThat(result.data().getLiabilities()).isEmpty();
    }
}
