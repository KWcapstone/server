package com.kwcapstone.Service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.File;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class S3Service {
    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucketName;

    private final String region = "ap-northeast-2";

    // S3 URL 생성 메서드
    public String getS3FileUrl(String s3Path) {
        return "https://" + bucketName + ".s3." + region + ".amazonaws.com/" + s3Path;
    }

    // 업로드
    public void uploadFileToS3(String s3Path, File file) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Path)
                .acl(ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
                .build();
        s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }

    // 다운로드
    public File getS3Data(String s3Path, String fileName) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Path).build();
        s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(Paths.get(fileName)));
        return new File(fileName);
    }

    // 삭제
    public void deleteS3WebEditorImage(String s3Path) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Path).build();
        s3Client.deleteObject(deleteObjectRequest);
    }

    // 복사?가 왜 필요한지 모르겠긴 함
    public void copyWebEditorImageToS3(String fromPath, String toPath) {
        CopyObjectRequest copyObjectRequest = CopyObjectRequest.builder()
                .sourceBucket(bucketName)
                .destinationBucket(bucketName)
                .sourceKey(fromPath)
                .destinationKey(toPath)
                .acl(ObjectCannedACL.BUCKET_OWNER_FULL_CONTROL)
                .build();
        s3Client.copyObject(copyObjectRequest);
    }
}
