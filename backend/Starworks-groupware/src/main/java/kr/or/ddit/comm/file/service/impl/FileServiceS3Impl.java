package kr.or.ddit.comm.file.service.impl;

import java.io.IOException;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import kr.or.ddit.comm.file.util.ObjectStorageUrlResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@Service
@RequiredArgsConstructor
@Slf4j
public class FileServiceS3Impl {

    private final S3Client s3Client;
    private final ObjectStorageUrlResolver objectStorageUrlResolver;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    public String uploadFile(MultipartFile file, String saveName, String folder) throws IOException {
        String key = objectStorageUrlResolver.buildObjectKey(folder, saveName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(file.getContentType())
            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));
        return saveName;
    }

    public String uploadPdf(byte[] fileBytes, String saveName, String folder, String contentType) throws IOException {
        String key = objectStorageUrlResolver.buildObjectKey(folder, saveName);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
            .bucket(bucketName)
            .key(key)
            .contentType(contentType)
            .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(fileBytes));
        return saveName;
    }

    public String getFileUrl(String saveName, String folder) {
        return objectStorageUrlResolver.buildObjectUrl(folder, saveName);
    }

    public void deleteFile(String saveName) {
        DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest.builder()
            .bucket(bucketName)
            .key(saveName)
            .build();

        s3Client.deleteObject(deleteObjectRequest);
    }

    public byte[] downloadFile(String key) throws IOException {
        return s3Client.getObject(
            GetObjectRequest.builder().bucket(bucketName).key(key).build(),
            ResponseTransformer.toBytes()
        ).asByteArray();
    }
}
