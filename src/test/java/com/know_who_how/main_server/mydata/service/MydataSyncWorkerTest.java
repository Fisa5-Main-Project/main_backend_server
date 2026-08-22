package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.mydata.dto.MydataDto;
import com.know_who_how.main_server.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MydataSyncWorkerTest {

    private static final Instant NOW = Instant.parse("2026-08-22T06:00:00Z");

    @Mock private MydataSyncJobService jobService;
    @Mock private UserRepository userRepository;
    @Mock private MydataService mydataService;
    @Mock private MydataSnapshotService snapshotService;
    @Mock private MydataTransactionExecutor transactionExecutor;
    @Mock private User user;

    private MydataSyncWorker worker;

    @BeforeEach
    void setUp() {
        worker = new MydataSyncWorker(
                jobService,
                userRepository,
                mydataService,
                snapshotService,
                transactionExecutor,
                Clock.fixed(NOW, ZoneOffset.UTC)
        );
        org.mockito.Mockito.lenient().doAnswer(invocation -> {
            Runnable action = invocation.getArgument(0);
            action.run();
            return null;
        }).when(transactionExecutor).execute(org.mockito.ArgumentMatchers.any(Runnable.class));
    }

    @Test
    @DisplayName("Worker가 RS 동기화와 Snapshot 저장을 완료하면 Job을 성공 처리한다")
    void execute_marksJobSucceeded() {
        MydataDto data = emptyData();
        when(jobService.getContext(1L)).thenReturn(new MydataSyncJobService.JobContext(1L, 7L, 0));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(mydataService.fetchMyDataInBackground(user)).thenReturn(data);

        worker.execute(1L);

        verify(mydataService).persistFetchedData(user, data);
        verify(snapshotService).upsert(7L, data, now());
        verify(jobService).markSucceeded(1L);
    }

    @Test
    @DisplayName("일시적 RS 장애는 Job 재시도로 전환한다")
    void execute_schedulesRetryOnServerError() {
        CustomException error = new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
        when(jobService.getContext(1L)).thenReturn(new MydataSyncJobService.JobContext(1L, 7L, 0));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(mydataService.fetchMyDataInBackground(user)).thenThrow(error);

        worker.execute(1L);

        verify(jobService).markRetry(1L, error);
    }

    @Test
    @DisplayName("Refresh Token이 만료되면 재시도하지 않고 재연동 필요로 처리한다")
    void execute_marksRelinkRequiredOnExpiredToken() {
        CustomException error = new CustomException(ErrorCode.MYDATA_EXPIRED);
        when(jobService.getContext(1L)).thenReturn(new MydataSyncJobService.JobContext(1L, 7L, 0));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(mydataService.fetchMyDataInBackground(user)).thenThrow(error);

        worker.execute(1L);

        verify(jobService).markRelinkRequired(1L, error);
    }

    @Test
    @DisplayName("Circuit OPEN이나 Bulkhead 포화는 시도 횟수를 소모하지 않고 Job을 뒤로 미룬다")
    void execute_defersWithoutAttemptWhenCallIsNotAllowed() {
        MydataCallDeferredException error = new MydataCallDeferredException("RS call is temporarily blocked");
        when(jobService.getContext(1L)).thenReturn(new MydataSyncJobService.JobContext(1L, 7L, 0));
        when(userRepository.findById(7L)).thenReturn(Optional.of(user));
        when(mydataService.fetchMyDataInBackground(user)).thenThrow(error);

        worker.execute(1L);

        verify(jobService).deferWithoutAttempt(1L, error);
    }

    private MydataDto emptyData() {
        MydataDto dto = new MydataDto();
        dto.setAssets(Collections.emptyList());
        dto.setLiabilities(Collections.emptyList());
        return dto;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(NOW, ZoneOffset.UTC);
    }
}
