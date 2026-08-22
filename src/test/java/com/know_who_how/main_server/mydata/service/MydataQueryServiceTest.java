package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.mydata.dto.MydataDto;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;

@ExtendWith(MockitoExtension.class)
class MydataQueryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-22T06:00:00Z");

    @Mock private MydataSnapshotService snapshotService;
    @Mock private MydataSyncJobService jobService;
    @Mock private MydataService mydataService;
    @Mock private MydataSyncLockService lockService;
    @Mock private MydataSnapshotWaiter snapshotWaiter;
    @Mock private MydataTransactionExecutor transactionExecutor;
    @Mock private User user;

    private MydataQueryService queryService;

    @BeforeEach
    void setUp() {
        queryService = new MydataQueryService(
                snapshotService,
                jobService,
                mydataService,
                lockService,
                snapshotWaiter,
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
    @DisplayName("최신 Snapshot이 있으면 RS를 호출하지 않고 즉시 반환한다")
    void getMyData_returnsFreshSnapshot() {
        MydataDto snapshotData = emptyData();
        when(user.getUserId()).thenReturn(1L);
        when(snapshotService.find(1L)).thenReturn(Optional.of(
                new MydataSnapshotService.SnapshotResult(snapshotData, now(), false)
        ));

        MydataDto result = queryService.getMyData(user);

        assertThat(result).isSameAs(snapshotData);
        verifyNoInteractions(jobService, mydataService);
    }

    @Test
    @DisplayName("stale Snapshot은 즉시 반환하고 중복 없는 Sync Job을 남긴다")
    void getMyData_returnsStaleSnapshotAndEnqueuesRefresh() {
        MydataDto snapshotData = emptyData();
        when(user.getUserId()).thenReturn(1L);
        when(snapshotService.find(1L)).thenReturn(Optional.of(
                new MydataSnapshotService.SnapshotResult(snapshotData, now().minusHours(2), true)
        ));

        MydataDto result = queryService.getMyData(user);

        assertThat(result).isSameAs(snapshotData);
        verify(jobService).enqueueIfAbsent(1L);
        verifyNoInteractions(mydataService);
    }

    @Test
    @DisplayName("Snapshot이 없으면 기존 동기 경로로 최초 데이터를 적재한다")
    void getMyData_fetchesAndStoresInitialSnapshot() {
        MydataDto fetched = emptyData();
        when(user.getUserId()).thenReturn(1L);
        when(snapshotService.find(1L)).thenReturn(Optional.empty());
        when(lockService.tryLock(eq(1L), anyString())).thenReturn(true);
        when(mydataService.fetchMyDataInline(user)).thenReturn(fetched);

        MydataDto result = queryService.getMyData(user);

        assertThat(result).isSameAs(fetched);
        verify(mydataService).persistFetchedData(user, fetched);
        verify(snapshotService).upsert(1L, fetched, now());
        verify(lockService).unlock(eq(1L), anyString());
        verify(jobService, never()).enqueueIfAbsent(1L);
    }

    @Test
    @DisplayName("최초 RS 호출이 실패하면 Job을 남기고 기존 예외를 그대로 반환한다")
    void getMyData_enqueuesJobWhenInitialFetchFails() {
        when(user.getUserId()).thenReturn(1L);
        when(snapshotService.find(1L)).thenReturn(Optional.empty());
        when(lockService.tryLock(eq(1L), anyString())).thenReturn(true);
        when(mydataService.fetchMyDataInline(user)).thenThrow(new CustomException(ErrorCode.MYDATA_SERVER_ERROR));

        assertThatThrownBy(() -> queryService.getMyData(user))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MYDATA_SERVER_ERROR);

        verify(jobService).enqueueIfAbsent(1L);
        verify(lockService).unlock(eq(1L), anyString());
    }

    @Test
    @DisplayName("다른 최초 요청이 동기화 중이면 RS를 중복 호출하지 않고 Snapshot을 기다린다")
    void getMyData_waitsForSnapshotWhenInitialSyncLockIsHeld() {
        MydataDto completedByOtherRequest = emptyData();
        when(user.getUserId()).thenReturn(1L);
        when(snapshotService.find(1L)).thenReturn(Optional.empty());
        when(lockService.tryLock(eq(1L), anyString())).thenReturn(false);
        when(snapshotWaiter.waitFor(1L)).thenReturn(Optional.of(completedByOtherRequest));

        MydataDto result = queryService.getMyData(user);

        assertThat(result).isSameAs(completedByOtherRequest);
        verifyNoInteractions(mydataService);
        verify(lockService, never()).unlock(eq(1L), anyString());
    }

    @Test
    @DisplayName("다른 최초 요청의 Snapshot이 시간 내 생성되지 않으면 기존 에러를 반환한다")
    void getMyData_failsWhenWaitingForInitialSnapshotTimesOut() {
        when(user.getUserId()).thenReturn(1L);
        when(snapshotService.find(1L)).thenReturn(Optional.empty());
        when(lockService.tryLock(eq(1L), anyString())).thenReturn(false);
        when(snapshotWaiter.waitFor(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> queryService.getMyData(user))
                .isInstanceOf(CustomException.class)
                .extracting("errorCode")
                .isEqualTo(ErrorCode.MYDATA_SERVER_ERROR);

        verify(jobService).enqueueIfAbsent(1L);
        verifyNoInteractions(mydataService);
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
