package com.know_who_how.main_server.mydata.service;

import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MydataSyncScheduler {

    private final MydataSyncJobService jobService;
    private final MydataSyncWorker worker;

    @Scheduled(
            fixedDelayString = "${mydata.sync.poll-delay:1s}",
            scheduler = "mydataTaskScheduler"
    )
    public void dispatch() {
        jobService.recoverStuckJobs();
        for (Long jobId : jobService.findDispatchableIds(10)) {
            if (jobService.claim(jobId)) {
                worker.execute(jobId);
            }
        }
    }
}
