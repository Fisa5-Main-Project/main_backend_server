package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.util.RedisUtil;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MydataSyncLockServiceTest {

    @Test
    @DisplayName("최초 동기화 락은 5초 TTL로 획득한다")
    void tryLock_usesFiveSecondTtl() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        MydataSyncLockService lockService = new MydataSyncLockService(redisUtil);
        when(redisUtil.setIfAbsent("mydata:sync:inflight:1", "owner", Duration.ofSeconds(5)))
                .thenReturn(true);

        assertThat(lockService.tryLock(1L, "owner")).isTrue();
    }

    @Test
    @DisplayName("락은 owner token이 일치할 때만 해제한다")
    void unlock_deletesOnlyOwnersLock() {
        RedisUtil redisUtil = mock(RedisUtil.class);
        MydataSyncLockService lockService = new MydataSyncLockService(redisUtil);

        lockService.unlock(1L, "owner");

        verify(redisUtil).deleteIfValueMatches("mydata:sync:inflight:1", "owner");
    }
}
