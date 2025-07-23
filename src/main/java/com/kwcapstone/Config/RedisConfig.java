//package com.kwcapstone.Config;
//
//import lombok.RequiredArgsConstructor;
//import org.redisson.Redisson;
//import org.redisson.api.RedissonClient;
//import org.redisson.config.Config;
//import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.data.redis.connection.RedisConfiguration;
//import org.springframework.data.redis.connection.RedisConnectionFactory;
//import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceClientConfiguration;
//import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
//import org.springframework.data.redis.core.RedisTemplate;
//
//@Configuration
//@RequiredArgsConstructor
//public class RedisConfig {
//    private final RedisProperties properties;
//
//    @Bean
//    public RedisConnectionFactory redisConnectionFactory() {
//        RedisConfiguration config = new RedisStandaloneConfiguration(properties.getHost(), properties.getPort());
//        LettuceClientConfiguration.LettuceClientConfigurationBuilder clientConfigurationBuilder = LettuceClientConfiguration.builder();
//
//        System.out.println("redisConnection: " + properties.getHost());
//        System.out.println("redisConnection: " + properties.getPort());
//        System.out.println("redisConnection: " + properties.getSsl().isEnabled());
//
//        if(Boolean.TRUE.equals(properties.getSsl().isEnabled())){
//            clientConfigurationBuilder.useSsl();
//        }
//
//        return new LettuceConnectionFactory(config, clientConfigurationBuilder.build());
//
//    }
//
//    @Bean
//    public RedisTemplate<String, String> redisTemplate() {
//        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
//        redisTemplate.setConnectionFactory(redisConnectionFactory());
//        return  redisTemplate;
//    }
//
//    @Bean
//    public RedissonClient redissonClient() {
//        Config config = new Config();
//        String url = String.format(createUrl(), properties.getHost(), properties.getPort());
//        System.out.println("redissonClient: " + url);
//        config.useSingleServer()
//                .setAddress(url)
//                .setSslEnableEndpointIdentification(false);
//
//        return Redisson.create(config);
//    }
//
//    private String createUrl(){
//        if(Boolean.TRUE.equals(properties.getSsl().isEnabled())){
//            return "rediss://%s:%d";
//        }
//        return "redis://%s:%d";
//    }
//}
