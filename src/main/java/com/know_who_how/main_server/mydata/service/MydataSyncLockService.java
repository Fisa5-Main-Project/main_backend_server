package com.know_who_how.main_server.mydata.service;

import com.know_who_how.main_server.global.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class MydataSyncLockService {

    private static final Duration LOCK_TTL = Duration.ofSeconds(5);
    private static final String KEY_PREFIX = "mydata:sync:inflight:";

    private final RedisUtil redisUtil;

    public boolean tryLock(Long userId, String ownerToken) {
        return redisUtil.setIfAbsent(key(userId), ownerToken, LOCK_TTL);
    }

    public void unlock(Long userId, String ownerToken) {
        redisUtil.deleteIfValueMatches(key(userId), ownerToken);
    }

    private String key(Long userId) {
        return KEY_PREFIX + userId;
    }
}
