package com.know_who_how.main_server.mydata.repository;

import com.know_who_how.main_server.mydata.entity.MydataSyncJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface MydataSyncJobRepository extends JpaRepository<MydataSyncJob, Long> {

    @Modifying
    @Query(value = """
            INSERT IGNORE INTO mydata_sync_job (
                user_id, status, attempt_count, next_retry_at, created_at, updated_at
            ) VALUES (
                :userId, 'QUEUED', 0, NOW(6), NOW(6), NOW(6)
            )
            """, nativeQuery = true)
    int insertQueuedIfAbsent(@Param("userId") Long userId);

    @Query("""
            SELECT job.id
            FROM MydataSyncJob job
            WHERE job.status IN :statuses
              AND job.nextRetryAt <= :now
            ORDER BY job.nextRetryAt, job.id
            """)
    List<Long> findDispatchableIds(
            @Param("statuses") List<com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus> statuses,
            @Param("now") LocalDateTime now,
            Pageable pageable
    );

    @Modifying
    @Query("""
            UPDATE MydataSyncJob job
            SET job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RUNNING,
                job.workerId = :workerId,
                job.startedAt = CURRENT_TIMESTAMP,
                job.updatedAt = CURRENT_TIMESTAMP
            WHERE job.id = :jobId
              AND job.status IN (
                  com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.QUEUED,
                  com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RETRY_WAIT,
                  com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.PERSIST_RETRY
              )
              AND job.nextRetryAt <= CURRENT_TIMESTAMP
            """)
    int claim(@Param("jobId") Long jobId, @Param("workerId") String workerId);

    @Modifying
    @Query("""
            UPDATE MydataSyncJob job
            SET job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.SUCCEEDED,
                job.completedAt = CURRENT_TIMESTAMP,
                job.updatedAt = CURRENT_TIMESTAMP
            WHERE job.id = :jobId
              AND job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RUNNING
            """)
    int markSucceeded(@Param("jobId") Long jobId);

    @Modifying
    @Query("""
            UPDATE MydataSyncJob job
            SET job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RETRY_WAIT,
                job.attemptCount = job.attemptCount + 1,
                job.nextRetryAt = :nextRetryAt,
                job.errorCode = :errorCode,
                job.errorMessage = :errorMessage,
                job.updatedAt = CURRENT_TIMESTAMP
            WHERE job.id = :jobId
              AND job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RUNNING
            """)
    int markRetry(
            @Param("jobId") Long jobId,
            @Param("nextRetryAt") LocalDateTime nextRetryAt,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage
    );

    @Modifying
    @Query("""
            UPDATE MydataSyncJob job
            SET job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RELINK_REQUIRED,
                job.errorCode = :errorCode,
                job.errorMessage = :errorMessage,
                job.completedAt = CURRENT_TIMESTAMP,
                job.updatedAt = CURRENT_TIMESTAMP
            WHERE job.id = :jobId
              AND job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RUNNING
            """)
    int markRelinkRequired(
            @Param("jobId") Long jobId,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage
    );

    @Modifying
    @Query("""
            UPDATE MydataSyncJob job
            SET job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RETRY_WAIT,
                job.nextRetryAt = :nextRetryAt,
                job.errorCode = :errorCode,
                job.errorMessage = :errorMessage,
                job.updatedAt = CURRENT_TIMESTAMP
            WHERE job.id = :jobId
              AND job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RUNNING
            """)
    int deferWithoutAttempt(
            @Param("jobId") Long jobId,
            @Param("nextRetryAt") LocalDateTime nextRetryAt,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage
    );

    @Modifying
    @Query("""
            UPDATE MydataSyncJob job
            SET job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.FAILED,
                job.errorCode = :errorCode,
                job.errorMessage = :errorMessage,
                job.completedAt = CURRENT_TIMESTAMP,
                job.updatedAt = CURRENT_TIMESTAMP
            WHERE job.id = :jobId
              AND job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RUNNING
            """)
    int markFailed(
            @Param("jobId") Long jobId,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage
    );

    @Modifying
    @Query("""
            UPDATE MydataSyncJob job
            SET job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RETRY_WAIT,
                job.nextRetryAt = CURRENT_TIMESTAMP,
                job.workerId = NULL,
                job.errorCode = 'MYDATA_WORKER_TIMEOUT',
                job.errorMessage = '작업 중단 감지 후 재시도',
                job.updatedAt = CURRENT_TIMESTAMP
            WHERE job.status = com.know_who_how.main_server.mydata.entity.MydataSyncJobStatus.RUNNING
              AND job.startedAt < :staleBefore
            """)
    int recoverStuckJobs(@Param("staleBefore") LocalDateTime staleBefore);
}
