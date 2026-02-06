package com.ssafy.meari.domain.kopic.service;

import com.ssafy.meari.domain.kopic.dto.request.KopicEvaluateRequest;
import com.ssafy.meari.domain.kopic.dto.response.KopicEvaluateResponse;
import com.ssafy.meari.domain.kopic.dto.response.KopicReportResponse;
import com.ssafy.meari.domain.kopic.dto.response.KopicTotalReportCreateResponse;
import com.ssafy.meari.domain.kopic.dto.response.KopicTotalReportResponse;
import com.ssafy.meari.domain.kopic.entity.KopicSentence;
import com.ssafy.meari.domain.kopic.repository.KopicSentenceRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.report.entity.KopicReport;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import com.ssafy.meari.domain.report.repository.KopicReportRepository;
import com.ssafy.meari.domain.report.repository.KopicTotalReportRepository;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import com.ssafy.meari.global.util.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Slf4j
@Service
@RequiredArgsConstructor
public class KopicEvaluateService {

    private final KopicSentenceRepository kopicSentenceRepository;
    private final KopicReportRepository kopicReportRepository;
    private final KopicTotalReportRepository kopicTotalReportRepository;
    private final ThemeRepository themeRepository;
    private final GeminiAnalysisService geminiAnalysisService;
    private final S3Service s3Service;

    @Transactional
    public KopicTotalReportCreateResponse createTotalReport(Member member, Long themeId) {
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_THEME));

        KopicTotalReport totalReport = KopicTotalReport.builder()
                .member(member)
                .theme(theme)
                .status(ReportStatus.PROCESSING)
                .build();

        kopicTotalReportRepository.save(totalReport);

        log.debug("코픽 통합 리포트 생성: totalReportId={}, memberId={}, themeId={}",
                totalReport.getKopicTotalReportId(), member.getMemberId(), themeId);

        return KopicTotalReportCreateResponse.from(totalReport);
    }

    @Transactional
    public KopicEvaluateResponse evaluate(Member member, KopicEvaluateRequest request, byte[] audioData, String contentType) {
        KopicTotalReport totalReport = kopicTotalReportRepository.findById(request.getKopicTotalReportId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_KOPIC_TOTAL_REPORT));

        KopicSentence sentence = kopicSentenceRepository.findById(request.getKopicSentenceId())
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_KOPIC_SENTENCE));

        // S3에 음성 파일 업로드
        String s3Key = s3Service.generateKopicKey(member.getMemberId(), sentence.getKopicSentenceId());
        s3Service.uploadFile(s3Key, audioData, contentType != null ? contentType : "audio/wav");

        // Presigned URL 발급 (GET용)
        String presignedUrl = s3Service.generatePresignedUrlForDownload(s3Key);
        log.debug("S3 업로드 완료 및 Presigned URL 발급: s3Key={}", s3Key);

        KopicReport report = KopicReport.builder()
                .member(member)
                .theme(sentence.getTheme())
                .kopicSentence(sentence)
                .kopicTotalReport(totalReport)
                .status(ReportStatus.PROCESSING)
                .build();

        kopicReportRepository.save(report);

        log.debug("코픽 분석 요청 생성: reportId={}, totalReportId={}, sentenceId={}",
                report.getKopicReportId(), totalReport.getKopicTotalReportId(), sentence.getKopicSentenceId());

        Long reportId = report.getKopicReportId();
        String textKo = sentence.getTextKo();
        Long totalReportId = totalReport.getKopicTotalReportId();

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                geminiAnalysisService.analyze(reportId, textKo, presignedUrl, totalReportId);
            }
        });

        return KopicEvaluateResponse.from(report);
    }

    @Transactional(readOnly = true)
    public KopicReportResponse getReport(Long reportId) {
        KopicReport report = kopicReportRepository.findById(reportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_KOPIC_REPORT));

        if (report.getStatus() == ReportStatus.PROCESSING) {
            return KopicReportResponse.processingFrom(report);
        }

        return KopicReportResponse.from(report);
    }

    @Transactional(readOnly = true)
    public KopicTotalReportResponse getTotalReport(Long totalReportId) {
        KopicTotalReport totalReport = kopicTotalReportRepository.findById(totalReportId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_KOPIC_TOTAL_REPORT));

        if (totalReport.getStatus() == ReportStatus.COMPLETED) {
            return KopicTotalReportResponse.from(totalReport);
        }

        long total = kopicReportRepository.countByKopicTotalReport(totalReport);
        long completed = kopicReportRepository.countByKopicTotalReportAndStatus(totalReport, ReportStatus.COMPLETED);
        return KopicTotalReportResponse.processingFrom(totalReportId, total, completed);
    }
}
