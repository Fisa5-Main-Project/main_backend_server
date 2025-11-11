package com.know_who_how.main_server.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class RedisUtil {

    private final RedisTemplate<String, Object> redisTemplate;

    public void save(String key, Object value) {
        redisTemplate.opsForValue().set(key, value);
    }

    // TTL을 설정하여 데이터를 저장하는 메소드 추가
    public void save(String key, Object value, Duration expireDuration) {
        redisTemplate.opsForValue().set(key, value, expireDuration);
    }

    // Key에 해당하는 값을 가져오는 메소드 추가
    public Object get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    // Key에 해당하는 데이터를 삭제하는 메소드 추가
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    // Key의 존재 여부를 확인하는 메소드 추가
    public Boolean hasKey(String key) {
        return redisTemplate.hasKey(key);
    }
}
