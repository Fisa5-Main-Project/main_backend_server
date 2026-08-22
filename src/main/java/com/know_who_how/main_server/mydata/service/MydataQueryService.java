package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.entity.User.User;
import com.know_who_how.main_server.mydata.dto.MydataDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class MydataQueryService {

    private final MydataSnapshotService snapshotService;
    private final MydataSyncJobService jobService;
    private final MydataService mydataService;
    private final MydataSyncLockService lockService;
    private final MydataSnapshotWaiter snapshotWaiter;
    private final MydataTransactionExecutor transactionExecutor;
    private final Clock clock;

    @Autowired
    public MydataQueryService(
            MydataSnapshotService snapshotService,
            MydataSyncJobService jobService,
            MydataService mydataService,
            MydataSyncLockService lockService,
            MydataSnapshotWaiter snapshotWaiter,
            MydataTransactionExecutor transactionExecutor
    ) {
        this(snapshotService, jobService, mydataService, lockService, snapshotWaiter,
                transactionExecutor, Clock.systemUTC());
    }

    MydataQueryService(
            MydataSnapshotService snapshotService,
            MydataSyncJobService jobService,
            MydataService mydataService,
            MydataSyncLockService lockService,
            MydataSnapshotWaiter snapshotWaiter,
            MydataTransactionExecutor transactionExecutor,
            Clock clock
    ) {
        this.snapshotService = snapshotService;
        this.jobService = jobService;
        this.mydataService = mydataService;
        this.lockService = lockService;
        this.snapshotWaiter = snapshotWaiter;
        this.transactionExecutor = transactionExecutor;
        this.clock = clock;
    }

    public MydataDto getMyData(User user) {
        Long userId = user.getUserId();
        var snapshot = snapshotService.find(userId);

        if (snapshot.isPresent()) {
            var result = snapshot.get();
            if (result.stale()) {
                jobService.enqueueIfAbsent(userId);
            }
            return result.data();
        }

        String ownerToken = UUID.randomUUID().toString();
        if (!lockService.tryLock(userId, ownerToken)) {
            return snapshotWaiter.waitFor(userId)
                    .orElseThrow(() -> {
                        jobService.enqueueIfAbsent(userId);
                        return new com.know_who_how.main_server.global.exception.CustomException(
                                com.know_who_how.main_server.global.exception.ErrorCode.MYDATA_SERVER_ERROR
                        );
                    });
        }

        try {
            try {
                MydataDto data = mydataService.fetchMyDataInline(user);
                if (data != null) {
                    transactionExecutor.execute(() -> {
                        mydataService.persistFetchedData(user, data);
                        snapshotService.upsert(userId, data, LocalDateTime.now(clock));
                    });
                }
                return data;
            } catch (MydataCallDeferredException e) {
                jobService.enqueueIfAbsent(userId);
                throw new com.know_who_how.main_server.global.exception.CustomException(
                        com.know_who_how.main_server.global.exception.ErrorCode.MYDATA_SERVER_ERROR
                );
            } catch (RuntimeException e) {
                jobService.enqueueIfAbsent(userId);
                throw e;
            }
        } finally {
            lockService.unlock(userId, ownerToken);
        }
    }
}
