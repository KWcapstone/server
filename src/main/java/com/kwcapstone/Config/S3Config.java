package com.kwcapstone.Config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ObjectCannedACL;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.File;

@Configuration
@RequiredArgsConstructor
public class S3Config {
    private final Environment env;
    @Bean
    public S3Client defaultS3Client() {
        return S3Client.builder().region(Region.AP_NORTHEAST_2).build();
    }

    // 업로드
    public void uploadFileToS3(String s3Path, File file) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(dataBucketName)
                .key(s3Path)
                .acl(ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
                .build();

    }

    // 다운로드
    public File getS3Data(String s3Path, String fileName) {
        GetObjectRequest objectRequest = GetObjectRequest.builder()
                .bucket(dataBucketName)
                .key(s3Path)
                .build();


    }
}
