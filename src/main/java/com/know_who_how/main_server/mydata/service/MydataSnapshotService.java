package com.know_who_how.main_server.mydata.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.mydata.dto.MydataDto;
import com.know_who_how.main_server.mydata.repository.MydataSnapshotRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MydataSnapshotService {

    private static final int CURRENT_SCHEMA_VERSION = 1;

    private final MydataSnapshotRepository snapshotRepository;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final Duration staleAfter;

    @Autowired
    public MydataSnapshotService(
            MydataSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper,
            @Value("${mydata.sync.stale-after:1d}") Duration staleAfter
    ) {
        this(snapshotRepository, objectMapper, Clock.systemUTC(), staleAfter);
    }

    MydataSnapshotService(
            MydataSnapshotRepository snapshotRepository,
            ObjectMapper objectMapper,
            Clock clock,
            Duration staleAfter
    ) {
        this.snapshotRepository = snapshotRepository;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.staleAfter = staleAfter;
    }

    @Transactional(readOnly = true)
    public Optional<SnapshotResult> find(Long userId) {
        return snapshotRepository.findById(userId)
                .map(snapshot -> new SnapshotResult(
                        deserialize(snapshot.getPayload()),
                        snapshot.getSyncedAt(),
                        snapshot.getSyncedAt().isBefore(now().minus(staleAfter))
                ));
    }

    @Transactional
    public void upsert(Long userId, MydataDto data, LocalDateTime sourceFetchedAt) {
        snapshotRepository.upsert(
                userId,
                serialize(data),
                CURRENT_SCHEMA_VERSION,
                sourceFetchedAt,
                now()
        );
    }

    private String serialize(MydataDto data) {
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        }
    }

    private MydataDto deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, MydataDto.class);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        }
    }

    private LocalDateTime now() {
        return LocalDateTime.now(clock);
    }

    public record SnapshotResult(MydataDto data, LocalDateTime syncedAt, boolean stale) {
    }
}
