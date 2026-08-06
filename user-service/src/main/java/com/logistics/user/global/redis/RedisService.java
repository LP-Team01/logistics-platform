package com.logistics.user.global.redis;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, String> redisTemplate;

    public void setValueTtl(UUID userId, String refreshToken, int day){
        redisTemplate.opsForValue().set(invertToKey(userId), refreshToken, Duration.ofDays(day));
    }

    public String getValue(UUID userId){
        return redisTemplate.opsForValue().get(invertToKey(userId));
    }

    public void delValue(UUID userId){
        redisTemplate.delete(invertToKey(userId));
    }

    private String invertToKey(UUID id){
        return "refreshToken:" + id;
    }
}
