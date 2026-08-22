package com.know_who_how.main_server.mydata.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;

@Component
public class MydataSnapshotWaiter {

    private final MydataSnapshotService snapshotService;
    private final Duration waitBudget;
    private final Duration pollInterval;

    public MydataSnapshotWaiter(
            MydataSnapshotService snapshotService,
            @Value("${mydata.sync.initial-wait-budget:2s}") Duration waitBudget,
            @Value("${mydata.sync.initial-poll-interval:200ms}") Duration pollInterval
    ) {
        this.snapshotService = snapshotService;
        this.waitBudget = waitBudget;
        this.pollInterval = pollInterval;
    }

    public Optional<com.know_who_how.main_server.mydata.dto.MydataDto> waitFor(Long userId) {
        long deadline = System.nanoTime() + waitBudget.toNanos();

        while (System.nanoTime() < deadline) {
            var snapshot = snapshotService.find(userId);
            if (snapshot.isPresent()) {
                return Optional.of(snapshot.get().data());
            }

            try {
                Thread.sleep(pollInterval.toMillis());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }

        return Optional.empty();
    }
}
