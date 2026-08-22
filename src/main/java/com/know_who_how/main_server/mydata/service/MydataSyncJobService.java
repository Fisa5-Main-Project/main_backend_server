package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.mydata.repository.MydataSyncJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class MydataSyncJobService {

    private final MydataSyncJobRepository jobRepository;

    @Transactional
    public boolean enqueueIfAbsent(Long userId) {
        return jobRepository.insertQueuedIfAbsent(userId) == 1;
    }

    @Transactional(readOnly = true)
    public List<Long> findDispatchableIds(int limit) {
        return jobRepository.findDispatchableIds(
                List.of(
                        com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.QUEUED,
                        com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RETRY_WAIT,
                        com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.PERSIST_RETRY
                ),
                LocalDateTime.now(),
                PageRequest.of(0, limit)
        );
    }

    @Transactional
    public boolean claim(Long jobId) {
        return jobRepository.claim(jobId, UUID.randomUUID().toString()) == 1;
    }

    @Transactional(readOnly = true)
    public JobContext getContext(Long jobId) {
        var job = jobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalStateException("MyData sync job not found: " + jobId));
        return new JobContext(job.getId(), job.getUserId(), job.getAttemptCount());
    }

    @Transactional
    public void markSucceeded(Long jobId) {
        jobRepository.markSucceeded(jobId);
    }

    @Transactional
    public void markRetry(Long jobId, RuntimeException error) {
        JobContext context = getContext(jobId);
        long delayMinutes = Math.min(1L << Math.min(context.attemptCount(), 3), 8L);
        String errorCode = error instanceof com.know_who_how.main_server.global.exception.CustomException custom
                ? custom.getErrorCode().getCode()
                : com.know_who_how.main_server.global.exception.ErrorCode.MYDATA_SERVER_ERROR.getCode();
        if (context.attemptCount() >= 4) {
            jobRepository.markFailed(jobId, errorCode, safeMessage(error));
            return;
        }
        jobRepository.markRetry(
                jobId,
                LocalDateTime.now().plusMinutes(delayMinutes),
                errorCode,
                safeMessage(error)
        );
    }

    @Transactional
    public void markRelinkRequired(
            Long jobId,
            com.know_who_how.main_server.global.exception.CustomException error
    ) {
        jobRepository.markRelinkRequired(
                jobId,
                error.getErrorCode().getCode(),
                safeMessage(error)
        );
    }

    @Transactional
    public void deferWithoutAttempt(Long jobId, RuntimeException error) {
        jobRepository.deferWithoutAttempt(
                jobId,
                LocalDateTime.now().plusSeconds(30),
                "MYDATA_CALL_DEFERRED",
                safeMessage(error)
        );
    }

    @Transactional
    public int recoverStuckJobs() {
        return jobRepository.recoverStuckJobs(LocalDateTime.now().minusMinutes(10));
    }

    private String safeMessage(Throwable error) {
        String message = error.getMessage();
        if (message == null) {
            return error.getClass().getSimpleName();
        }
        return message.length() <= 1000 ? message : message.substring(0, 1000);
    }

    public record JobContext(Long jobId, Long userId, int attemptCount) {
    }
}
