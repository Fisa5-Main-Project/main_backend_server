package com.know_who_how.main_server.inheritance.service;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class S3Service {

    // AwsS3Config에서 등록한 AmazonS3 클라이언트 주입받음.
    private final AmazonS3 amazonS3;

    @Value("${spring.cloud.aws.s3.bucket}")
    private String bucketName;

    private static final long MULTIPART_UPLOAD_URL_EXPIRATION_MILLIS = Duration.ofMinutes(30).toMillis(); // Part URL 유효기간: 30분
    private static final long DOWNLOAD_URL_EXPIRATION_MILLIS = Duration.ofMinutes(5).toMillis();

    /**
     * [1] 영상 업로드 전 S3에서 UploadId 받아오기
     * @param objectKey S3 저장 경로 (서비스에서 videoKey를 넘김)
     * @return S3에서 발급한 Upload ID
     */
    public String initiateMultipartUpload(String objectKey){
        InitiateMultipartUploadRequest request = new InitiateMultipartUploadRequest(bucketName, objectKey);
        InitiateMultipartUploadResult result = amazonS3.initiateMultipartUpload(request);
        return result.getUploadId();
    }

    /**
     * [2] Part Upload용 Presigned URL 생성
     * @param objectKey S3 저장 경로(videoId 기반)
     * @param uploadId initiate 단계에서 받은 Upload ID
     * @param partNumber 업로드할 조각 번호 (1부터 시작)
     * @return Part PUT 요청용 Presigned URL
     * ex) return 값 예시
     * https://{bucket-name}.s3.{region}.amazonaws.com/{object-key}
     * ?AWSAccessKeyId={Access Key ID}
     * &Expires={Expiration Timestamp}
     * &Signature={Generated Signature}
     * &uploadId={Upload ID}
     * &partNumber={Part Number}
     */
    public String generatePartPresignedUrl(String objectKey, String uploadId, int partNumber){
        Date expiration = new Date(System.currentTimeMillis() + MULTIPART_UPLOAD_URL_EXPIRATION_MILLIS);

        GeneratePresignedUrlRequest generatePresignedUrlRequest
                = new GeneratePresignedUrlRequest(bucketName, objectKey)
                .withMethod(HttpMethod.PUT)
                .withExpiration(expiration);

        // Multipart Upload를 위한 Part Number와 Upload ID를 요청 파라미터로 추가
        generatePresignedUrlRequest.addRequestParameter("uploadId", uploadId);
        generatePresignedUrlRequest.addRequestParameter("partNumber", String.valueOf(partNumber));

        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString();
    }

    /**
     * [3] Multipart Upload 완료 (Completion)
     * @param objectKey S3 저장 경로 (video 키 기반)
     * @param uploadId initiate 단계에서 받은 Upload ID
     * @param partETags 클라이언트로부터 받은 모든 Part ETag 리스트
     */
    public void completeMultipartUpload(String objectKey, String uploadId, List<PartETag> partETags){
        CompleteMultipartUploadRequest request =
                new CompleteMultipartUploadRequest(bucketName, objectKey, uploadId, partETags);

        amazonS3.completeMultipartUpload(request);
    }

    // S3 다운로드를 위한 Presigned URL을 생성 (GET 권한)
    // 백엔드 검증 후 클라이언트를 S3로 리다이렉트
    public String generateDownloadPresignedUrl(String objectKey){
        Date expiration = new Date(System.currentTimeMillis()+DOWNLOAD_URL_EXPIRATION_MILLIS);


        // 캐싱 방지 HTTP 헤더 설정: 클라이언트(브라우저/CDN)가 이 응답을 캐시하지 않도록 강제
        ResponseHeaderOverrides overrides = new ResponseHeaderOverrides()
                .withCacheControl("no-cache, no-store, must-revalidate")
                .withExpires("0");

        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withMethod(HttpMethod.GET)
                        .withExpiration(expiration)
                        .withResponseHeaders(overrides);

        return amazonS3.generatePresignedUrl(generatePresignedUrlRequest).toString();
    }

    public void deleteObject(String objectKey){
        DeleteObjectRequest deleteObjectRequest = new DeleteObjectRequest(bucketName, objectKey);
        amazonS3.deleteObject(deleteObjectRequest);
    }
}
