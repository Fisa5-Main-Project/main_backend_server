package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.mydata.repository.MydataSyncJobRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MydataSyncJobServiceTest {

    @Mock
    private MydataSyncJobRepository jobRepository;

    @InjectMocks
    private MydataSyncJobService jobService;

    @Test
    @DisplayName("활성 Job이 없으면 QUEUED Job을 생성한다")
    void enqueueIfAbsent_returnsTrueWhenInserted() {
        when(jobRepository.insertQueuedIfAbsent(1L)).thenReturn(1);

        assertThat(jobService.enqueueIfAbsent(1L)).isTrue();
    }

    @Test
    @DisplayName("이미 활성 Job이 있으면 중복 Job을 생성하지 않는다")
    void enqueueIfAbsent_returnsFalseWhenActiveJobExists() {
        when(jobRepository.insertQueuedIfAbsent(1L)).thenReturn(0);

        assertThat(jobService.enqueueIfAbsent(1L)).isFalse();
    }

    @Test
    @DisplayName("조건부 UPDATE가 1건이면 Job 선점에 성공한다")
    void claim_returnsTrueOnlyWhenUpdated() {
        when(jobRepository.claim(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.anyString()
        )).thenReturn(1);

        assertThat(jobService.claim(10L)).isTrue();
    }

    @Test
    @DisplayName("실제 RS 호출 실패가 5회째면 Job을 최종 실패 처리한다")
    void markRetry_marksFailedAfterFifthFailure() {
        var job = org.mockito.Mockito.mock(com.know_who_how.main_server.mydata.entity.MydataSyncJob.class);
        when(job.getId()).thenReturn(10L);
        when(job.getUserId()).thenReturn(1L);
        when(job.getAttemptCount()).thenReturn(4);
        when(jobRepository.findById(10L)).thenReturn(java.util.Optional.of(job));
        var error = new com.know_who_how.main_server.global.exception.CustomException(
                com.know_who_how.main_server.global.exception.ErrorCode.MYDATA_SERVER_ERROR
        );

        jobService.markRetry(10L, error);

        verify(jobRepository).markFailed(
                org.mockito.ArgumentMatchers.eq(10L),
                org.mockito.ArgumentMatchers.eq("MYDATA_003"),
                org.mockito.ArgumentMatchers.anyString()
        );
    }
}
