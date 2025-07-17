package com.kwcapstone.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConfiguration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;

@Configuration
@RequiredArgsConstructor
public class RedisConfig {
    private final RedisProperties properties;

    @Bean
    public RedisConnectionFactory redisConnectionFactory() {
        RedisConfiguration config = new RedisStandaloneConfiguration(properties.getHost(), properties.getPort());
        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigurationBuilder = LettuceClientConfiguration.builder();

        if(Boolean.TRUE.equals(properties.getSsl().isEnabled())){
            clientConfigurationBuilder.useSsl();
        }

        return new LettuceConnectionFactory(config, clientConfigurationBuilder.build());

    }

    @Bean
    public RedisTemplate<String, String> redisTemplate() {
        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(redisConnectionFactory());
        return  redisTemplate;
    }

    @Bean
    public RedissionClient
}
