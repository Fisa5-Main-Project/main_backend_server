package com.know_who_how.main_server.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

/**
 * 앱 전역(MyData 외) @Scheduled 메서드가 쓰는 기본 스케줄러.
 * "taskScheduler"라는 이름은 ScheduledAnnotationBeanPostProcessor가
 * scheduler 속성 없는 @Scheduled를 해석할 때 우선적으로 찾는 빈 이름이라,
 * MydataAsyncConfig#mydataTaskScheduler와 스레드 풀이 분리된다.
 */
@Configuration
public class SchedulingConfig {

    @Bean("taskScheduler")
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
        scheduler.setPoolSize(2);
        scheduler.setThreadNamePrefix("app-scheduler-");
        scheduler.setWaitForTasksToCompleteOnShutdown(true);
        scheduler.initialize();
        return scheduler;
    }
}
