package com.ssafy.meari.global.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;

/**
 * AWS S3 서비스
 * - Presigned URL 생성
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;

    @Value("${cloud.aws.s3.bucket}")
    private String bucketName;

    @Value("${cloud.aws.presigned-url.video-expiration}")
    private long videoExpirationSeconds;

    @Value("${cloud.aws.presigned-url.upload-expiration}")
    private long uploadExpirationSeconds;

    /**
     * 동영상 시청용 Presigned URL 생성 (GET)
     * @param s3Key S3 객체 키 (예: "videos/1/video.mp4")
     * @return Presigned URL
     */
    public String generatePresignedUrlForVideo(String s3Key) {
        log.debug("동영상 Presigned URL 생성 시작: bucket={}, key={}", bucketName, s3Key);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(videoExpirationSeconds))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        String url = presignedRequest.url().toString();

        log.info("동영상 Presigned URL 생성 완료: key={}, expiresIn={}s", s3Key, videoExpirationSeconds);
        return url;
    }

    /**
     * 파일 업로드용 Presigned URL 생성 (PUT)
     * @param s3Key S3 객체 키 (예: "recordings/1/round1/10/123.wav")
     * @return Presigned URL
     */
    public String generatePresignedUrlForUpload(String s3Key) {
        log.debug("업로드 Presigned URL 생성 시작: bucket={}, key={}", bucketName, s3Key);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(uploadExpirationSeconds))
                .putObjectRequest(putObjectRequest)
                .build();

        PresignedPutObjectRequest presignedRequest = s3Presigner.presignPutObject(presignRequest);
        String url = presignedRequest.url().toString();

        log.info("업로드 Presigned URL 생성 완료: key={}, expiresIn={}s", s3Key, uploadExpirationSeconds);
        return url;
    }

    /**
     * S3 키 생성 - 녹음 파일용
     * @param roomId 방 ID
     * @param round 라운드 번호
     * @param memberId 멤버 ID
     * @param sentenceId 문장 ID
     * @return S3 키 (예: "recordings/1/round1/10/123.wav")
     */
    public String generateRecordingKey(Long roomId, Integer round, Long memberId, Long sentenceId) {
        return String.format("recordings/%d/round%d/%d/%d.wav", roomId, round, memberId, sentenceId);
    }

    /**
     * S3 키 생성 - 코픽 음성 파일용
     * @param memberId 멤버 ID
     * @param kopicSentenceId 코픽 문장 ID
     * @return S3 키 (예: "kopic/1/501.wav")
     */
    public String generateKopicKey(Long memberId, Long kopicSentenceId) {
        return String.format("kopic/%d/%d_%d.wav", memberId, kopicSentenceId, System.currentTimeMillis());
    }

    /**
     * 파일 업로드 (바이트 배열)
     * @param s3Key S3 객체 키
     * @param data 파일 데이터
     * @param contentType 콘텐츠 타입
     */
    public void uploadFile(String s3Key, byte[] data, String contentType) {
        log.debug("S3 파일 업로드 시작: bucket={}, key={}, size={}", bucketName, s3Key, data.length);

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .contentType(contentType)
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(data));

        log.info("S3 파일 업로드 완료: key={}", s3Key);
    }

    /**
     * 파일 다운로드용 Presigned URL 생성 (GET)
     * @param s3Key S3 객체 키
     * @return Presigned URL
     */
    public String generatePresignedUrlForDownload(String s3Key) {
        log.debug("다운로드 Presigned URL 생성 시작: bucket={}, key={}", bucketName, s3Key);

        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(videoExpirationSeconds))
                .getObjectRequest(getObjectRequest)
                .build();

        PresignedGetObjectRequest presignedRequest = s3Presigner.presignGetObject(presignRequest);
        String url = presignedRequest.url().toString();

        log.info("다운로드 Presigned URL 생성 완료: key={}, expiresIn={}s", s3Key, videoExpirationSeconds);
        return url;
    }
}
