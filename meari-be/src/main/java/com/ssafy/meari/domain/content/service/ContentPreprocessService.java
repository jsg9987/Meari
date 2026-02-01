package com.ssafy.meari.domain.content.service;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.SentenceRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import ws.schild.jave.*;
import ws.schild.jave.encode.AudioAttributes;
import ws.schild.jave.encode.EncodingAttributes;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentPreprocessService {

    private final ContentRepository contentRepository;
    private final SentenceRepository sentenceRepository;
    private final S3Client s3Client;

    @Value("${cloud.aws.s3.bucket}")
    private String bucket;

    /**
     * Content의 동영상을 문장별로 잘라서 S3에 업로드
     */
    @Transactional
    public void preprocessContentAudio(Long contentId) {
        log.info("Content {} 오디오 전처리 시작", contentId);

        // 1. Content 조회
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONTENT));

        String videoUrl = content.getVideoUrl();
        if (videoUrl == null || videoUrl.isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
        }

        // 2. Sentence 목록 조회
        List<Sentence> sentences = sentenceRepository.findByContent(content);
        log.info("Content {} 문장 수: {}", contentId, sentences.size());

        // 3. 동영상 다운로드
        File videoFile = null;
        try {
            videoFile = downloadVideo(videoUrl);
            log.info("동영상 다운로드 완료: {}", videoFile.getAbsolutePath());

            // 4. 각 문장 처리
            int successCount = 0;
            for (Sentence sentence : sentences) {
                try {
                    processSentence(contentId, videoFile, sentence);
                    successCount++;
                    log.debug("문장 {} 처리 완료 ({}/{})",
                            sentence.getSentenceId(), successCount, sentences.size());
                } catch (Exception e) {
                    log.error("문장 {} 처리 실패: {}", sentence.getSentenceId(), e.getMessage(), e);
                }
            }

            log.info("Content {} 전처리 완료: {}/{} 성공",
                    contentId, successCount, sentences.size());

        } catch (Exception e) {
            log.error("Content {} 전처리 실패: {}", contentId, e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            // 임시 파일 정리
            if (videoFile != null && videoFile.exists()) {
                videoFile.delete();
            }
        }
    }

    /**
     * 개별 문장 처리
     */
    private void processSentence(Long contentId, File videoFile, Sentence sentence) {
        File audioFile = null;
        try {
            // 1. FFmpeg로 구간 추출
            audioFile = extractAudio(
                    videoFile,
                    sentence.getStartTime(),
                    sentence.getEndTime()
            );

            // 2. S3 업로드
            String s3Key = String.format(
                    "reference_audio/%d/%d.wav",
                    contentId,
                    sentence.getSentenceId()
            );

            uploadToS3(audioFile, s3Key);
            log.debug("S3 업로드 완료: {}", s3Key);

            // 3. DB 업데이트
            sentence.updateReferenceAudioKey(s3Key);

        } finally {
            // 임시 파일 정리
            if (audioFile != null && audioFile.exists()) {
                audioFile.delete();
            }
        }
    }

    /**
     * 동영상 다운로드
     */
    private File downloadVideo(String videoUrl) throws IOException, InterruptedException {
        log.debug("동영상 다운로드 시작: {}", videoUrl);

        HttpClient client = HttpClient.newHttpClient();
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(videoUrl))
                .GET()
                .build();

        Path tempFile = Files.createTempFile("video_", ".mp4");
        HttpResponse<InputStream> response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());

        try (InputStream in = response.body();
             FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = in.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
        }

        return tempFile.toFile();
    }

    /**
     * FFmpeg로 오디오 구간 추출
     */
    private File extractAudio(File videoFile, BigDecimal startTime, BigDecimal endTime) {
        try {
            File outputFile = Files.createTempFile("audio_", ".wav").toFile();

            MultimediaObject source = new MultimediaObject(videoFile);

            // 오디오 설정
            AudioAttributes audio = new AudioAttributes();
            audio.setCodec("pcm_s16le");  // WAV format
            audio.setChannels(1);          // Mono
            audio.setSamplingRate(16000);  // 16kHz

            // 인코딩 설정
            EncodingAttributes attrs = new EncodingAttributes();
            attrs.setOutputFormat("wav");
            attrs.setAudioAttributes(audio);
            attrs.setOffset(startTime.floatValue());  // 시작 시간
            attrs.setDuration(endTime.subtract(startTime).floatValue());  // 길이

            // 인코딩 실행
            Encoder encoder = new Encoder();
            encoder.encode(source, outputFile, attrs);

            return outputFile;

        } catch (IOException | EncoderException e) {
            log.error("오디오 추출 실패: {}", e.getMessage(), e);
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * S3에 파일 업로드
     */
    private void uploadToS3(File file, String s3Key) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(s3Key)
                .contentType("audio/wav")
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromFile(file));
    }
}
