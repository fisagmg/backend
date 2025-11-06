package com.labhub.CveLabhubBack.report.util;

import com.labhub.CveLabhubBack.report.exception.CustomStorageException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Component
@RequiredArgsConstructor
@Profile("!test")
public class S3StorageUtil {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    @Value("${aws.s3.template-path:template/report_base.docx}")
    private String templatePath;

    /**
     * 템플릿 파일을 복사하여 새로운 보고서 생성
     */
    public String copyTemplateToNewReport(Long userId, String cveId) {
        try {
            String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
            String destinationKey = String.format("reports/%d/%s/%s_v1.docx", userId, cveId, timestamp);

            CopyObjectRequest copyRequest = CopyObjectRequest.builder()
                    .sourceBucket(bucketName)
                    .sourceKey(templatePath)
                    .destinationBucket(bucketName)
                    .destinationKey(destinationKey)
                    .build();

            s3Client.copyObject(copyRequest);
            
            log.info("Template copied successfully to: {}", destinationKey);
            return destinationKey;
            
        } catch (S3Exception e) {
            log.error("Failed to copy template from S3", e);
            throw new CustomStorageException("템플릿 복사 실패: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while copying template", e);
            throw new CustomStorageException("템플릿 복사 중 오류 발생", e);
        }
    }

    /**
     * 파일을 S3에 업로드 (덮어쓰기 또는 새 버전)
     */
    public String uploadFile(MultipartFile file, String s3Key) {
        try {
            PutObjectRequest putRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .contentType("application/vnd.openxmlformats-officedocument.wordprocessingml.document")
                    .build();

            s3Client.putObject(putRequest, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            
            log.info("File uploaded successfully to: {}", s3Key);
            return s3Key;
            
        } catch (S3Exception e) {
            log.error("Failed to upload file to S3", e);
            throw new CustomStorageException("파일 업로드 실패: " + e.awsErrorDetails().errorMessage(), e);
        } catch (IOException e) {
            log.error("Failed to read file input stream", e);
            throw new CustomStorageException("파일 읽기 실패", e);
        } catch (Exception e) {
            log.error("Unexpected error while uploading file", e);
            throw new CustomStorageException("파일 업로드 중 오류 발생", e);
        }
    }

    /**
     * 새 버전의 파일 키 생성
     */
    public String generateNewVersionKey(String currentKey, int newVersion) {
        // 예: reports/1/101/20251106_145001_v1.docx -> reports/1/101/20251106_145001_v2.docx
        String withoutExtension = currentKey.substring(0, currentKey.lastIndexOf(".docx"));
        String baseKey = withoutExtension.substring(0, withoutExtension.lastIndexOf("_v"));
        return String.format("%s_v%d.docx", baseKey, newVersion);
    }

    /**
     * Presigned URL 생성 (다운로드용)
     */
    public String generatePresignedUrl(String s3Key) {
        try {
            GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                    .signatureDuration(Duration.ofMinutes(5))
                    .getObjectRequest(getObjectRequest)
                    .build();

            PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
            String url = presignedRequest.url().toString();
            
            log.info("Presigned URL generated for: {}", s3Key);
            return url;
            
        } catch (S3Exception e) {
            log.error("Failed to generate presigned URL", e);
            throw new CustomStorageException("다운로드 URL 생성 실패: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while generating presigned URL", e);
            throw new CustomStorageException("URL 생성 중 오류 발생", e);
        }
    }

    /**
     * S3에서 파일 삭제
     */
    public void deleteFile(String s3Key) {
        try {
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.deleteObject(deleteRequest);
            
            log.info("File deleted successfully: {}", s3Key);
            
        } catch (S3Exception e) {
            log.error("Failed to delete file from S3", e);
            throw new CustomStorageException("파일 삭제 실패: " + e.awsErrorDetails().errorMessage(), e);
        } catch (Exception e) {
            log.error("Unexpected error while deleting file", e);
            throw new CustomStorageException("파일 삭제 중 오류 발생", e);
        }
    }

    /**
     * 파일이 S3에 존재하는지 확인
     */
    public boolean fileExists(String s3Key) {
        try {
            HeadObjectRequest headRequest = HeadObjectRequest.builder()
                    .bucket(bucketName)
                    .key(s3Key)
                    .build();

            s3Client.headObject(headRequest);
            return true;
            
        } catch (NoSuchKeyException e) {
            return false;
        } catch (S3Exception e) {
            log.error("Error checking file existence", e);
            throw new CustomStorageException("파일 존재 확인 실패", e);
        }
    }

    /**
     * 전체 S3 URL 생성
     */
    public String getFullS3Url(String s3Key) {
        return String.format("https://%s.s3.amazonaws.com/%s", bucketName, s3Key);
    }
}

