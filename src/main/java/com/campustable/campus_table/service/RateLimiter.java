package com.campustable.campus_table.service;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis 기반 카운터. 서버가 여러 대여도 같은 한도를 공유한다.
 * 증가와 만료 설정을 Lua 스크립트로 한 번에 처리해, 사이에 장애가 나도 만료 없는 키가 남지 않는다.
 */
@Component
@RequiredArgsConstructor
public class RateLimiter {

    private static final DefaultRedisScript<Long> INCREMENT = new DefaultRedisScript<>(
            "local c = redis.call('INCR', KEYS[1]) "
                    + "if c == 1 or redis.call('TTL', KEYS[1]) < 0 then redis.call('EXPIRE', KEYS[1], ARGV[1]) end "
                    + "return c", Long.class);

    private final StringRedisTemplate redis;

    /** 카운터를 1 올리고 올린 뒤의 값을 반환한다. 첫 증가 때 window 만큼의 만료가 걸린다. */
    public long increment(String key, Duration window) {
        Long count = redis.execute(INCREMENT, List.of(key), String.valueOf(window.toSeconds()));
        return count == null ? 0 : count;
    }

    public long count(String key) {
        String value = redis.opsForValue().get(key);
        return value == null ? 0 : Long.parseLong(value);
    }

    /** 키가 없을 때만 만들고 true. 이미 있으면 false (쿨다운 용도). */
    public boolean tryAcquire(String key, Duration ttl) {
        return Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(key, "1", ttl));
    }

    /** 키가 풀리기까지 남은 초 (최소 1) */
    public long retryAfterSeconds(String key) {
        Long ttl = redis.getExpire(key, TimeUnit.SECONDS);
        return ttl == null || ttl < 1 ? 1 : ttl;
    }

    public void reset(String key) {
        redis.delete(key);
    }
}
