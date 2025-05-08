//package com.kwcapstone.Config;
//
//import lombok.RequiredArgsConstructor;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.core.env.Environment;
//import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
//import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
//import software.amazon.awssdk.regions.Region;
//import software.amazon.awssdk.services.s3.S3Client;
//
//@Configuration
//@RequiredArgsConstructor
//public class S3ClientConfig {
//    private final Environment env;
//    @Bean
//    public S3Client amazonS3() {
//        AwsBasicCredentials credentials = AwsBasicCredentials.create(
//                env.getProperty("cloud.s3.access-id"), env.getProperty("cloud.s3.secret-key"));
//        return S3Client.builder()
//                .region(Region.AP_NORTHEAST_2)
//                .credentialsProvider(StaticCredentialsProvider.create(credentials))
//                .build();
//    }
//}
