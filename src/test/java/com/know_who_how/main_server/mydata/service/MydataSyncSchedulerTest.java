package com.know_who_how.main_server.mydata.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MydataSyncSchedulerTest {

    @Test
    @DisplayName("조건부 UPDATE로 선점에 성공한 Job만 Worker에 전달한다")
    void dispatch_executesOnlyClaimedJobs() {
        MydataSyncJobService jobService = mock(MydataSyncJobService.class);
        MydataSyncWorker worker = mock(MydataSyncWorker.class);
        MydataSyncScheduler scheduler = new MydataSyncScheduler(jobService, worker);
        when(jobService.findDispatchableIds(10)).thenReturn(List.of(1L, 2L));
        when(jobService.claim(1L)).thenReturn(true);
        when(jobService.claim(2L)).thenReturn(false);

        scheduler.dispatch();

        verify(jobService).recoverStuckJobs();
        verify(worker).execute(1L);
        verify(worker, never()).execute(2L);
    }
}
