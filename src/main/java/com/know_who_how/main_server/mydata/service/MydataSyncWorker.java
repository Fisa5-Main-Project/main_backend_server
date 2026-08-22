package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.exception.CustomException;
import com.know_who_how.main_server.global.exception.ErrorCode;
import com.know_who_how.main_server.user.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;

@Service
public class MydataSyncWorker {

    private final MydataSyncJobService jobService;
    private final UserRepository userRepository;
    private final MydataService mydataService;
    private final MydataSnapshotService snapshotService;
    private final MydataTransactionExecutor transactionExecutor;
    private final Clock clock;

    @Autowired
    public MydataSyncWorker(
            MydataSyncJobService jobService,
            UserRepository userRepository,
            MydataService mydataService,
            MydataSnapshotService snapshotService,
            MydataTransactionExecutor transactionExecutor
    ) {
        this(jobService, userRepository, mydataService, snapshotService,
                transactionExecutor, Clock.systemUTC());
    }

    MydataSyncWorker(
            MydataSyncJobService jobService,
            UserRepository userRepository,
            MydataService mydataService,
            MydataSnapshotService snapshotService,
            MydataTransactionExecutor transactionExecutor,
            Clock clock
    ) {
        this.jobService = jobService;
        this.userRepository = userRepository;
        this.mydataService = mydataService;
        this.snapshotService = snapshotService;
        this.transactionExecutor = transactionExecutor;
        this.clock = clock;
    }

    @Async("mydataSyncExecutor")
    public void execute(Long jobId) {
        var context = jobService.getContext(jobId);
        var user = userRepository.findById(context.userId())
                .orElseThrow(() -> new IllegalStateException("User not found: " + context.userId()));

        try {
            var data = mydataService.fetchMyDataInBackground(user);
            if (data == null) {
                throw new CustomException(ErrorCode.MYDATA_SERVER_ERROR);
            }
            transactionExecutor.execute(() -> {
                mydataService.persistFetchedData(user, data);
                snapshotService.upsert(context.userId(), data, LocalDateTime.now(clock));
                jobService.markSucceeded(jobId);
            });
        } catch (MydataCallDeferredException error) {
            jobService.deferWithoutAttempt(jobId, error);
        } catch (CustomException error) {
            if (error.getErrorCode() == ErrorCode.MYDATA_EXPIRED) {
                jobService.markRelinkRequired(jobId, error);
            } else {
                jobService.markRetry(jobId, error);
            }
        } catch (RuntimeException error) {
            jobService.markRetry(jobId, error);
        }
    }
}
