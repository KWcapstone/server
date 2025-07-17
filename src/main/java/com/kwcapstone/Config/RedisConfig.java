package com.kwcapstone.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.redis.RedisConnectionDetails;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final RedisProperties properties;

    @Bean
    public RedisConnectionDetails redisConnectionDetails() {
        RedisConfigu
    }
}
