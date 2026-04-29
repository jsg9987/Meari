package com.ssafy.meari.domain.kopic.service;

import com.ssafy.meari.domain.kopic.dto.request.KopicEvaluateRequest;
import com.ssafy.meari.domain.kopic.dto.response.KopicEvaluateResponse;
import com.ssafy.meari.domain.kopic.dto.response.KopicReportResponse;
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
import com.ssafy.meari.global.error.exception.BusinessException;
import com.ssafy.meari.global.util.S3Service;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * KopicEvaluateService 단위 테스트
 * - 시그니처: evaluate(member, request, byte[] audioData, String contentType)
 * - S3 업로드 후 트랜잭션 커밋 시점(afterCommit)에 GeminiAnalysisService 호출
 * - TransactionSynchronizationManager는 직접 init/clear 해서 콜백을 시뮬레이트
 */
@ExtendWith(MockitoExtension.class)
class KopicEvaluateServiceTest {

    @InjectMocks
    private KopicEvaluateService kopicEvaluateService;

    @Mock
    private KopicSentenceRepository kopicSentenceRepository;

    @Mock
    private KopicReportRepository kopicReportRepository;

    @Mock
    private KopicTotalReportRepository kopicTotalReportRepository;

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private GeminiAnalysisService geminiAnalysisService;

    @Mock
    private S3Service s3Service;

    @AfterEach
    void clearTxSync() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Nested
    @DisplayName("evaluate - 코픽 발화 분석 요청")
    class EvaluateTest {

        @Test
        @DisplayName("성공: 리포트 PROCESSING 생성 + S3 업로드 + 트랜잭션 커밋 후 Gemini 호출")
        void evaluate_success() {
            // Given
            Member member = mock(Member.class);
            given(member.getMemberId()).willReturn(1L);

            Theme theme = mock(Theme.class);

            KopicSentence sentence = mock(KopicSentence.class);
            given(sentence.getKopicSentenceId()).willReturn(501L);
            given(sentence.getTheme()).willReturn(theme);
            given(sentence.getTextKo()).willReturn("오늘 점심 메뉴는 뭐예요?");

            KopicTotalReport totalReport = mock(KopicTotalReport.class);
            given(totalReport.getKopicTotalReportId()).willReturn(2001L);

            KopicEvaluateRequest request = mock(KopicEvaluateRequest.class);
            given(request.getKopicTotalReportId()).willReturn(2001L);
            given(request.getKopicSentenceId()).willReturn(501L);

            byte[] audioData = new byte[]{1, 2, 3, 4};
            String contentType = "audio/wav";
            String s3Key = "kopic/1/501_123456.wav";
            String presignedUrl = "https://s3.../" + s3Key;

            given(kopicTotalReportRepository.findById(2001L)).willReturn(Optional.of(totalReport));
            given(kopicSentenceRepository.findById(501L)).willReturn(Optional.of(sentence));
            given(kopicReportRepository.save(any(KopicReport.class))).willAnswer(invocation -> {
                KopicReport report = invocation.getArgument(0);
                ReflectionTestUtils.setField(report, "kopicReportId", 1001L);
                return report;
            });
            given(s3Service.generateKopicKey(1L, 501L)).willReturn(s3Key);
            given(s3Service.generatePresignedUrlForDownload(s3Key)).willReturn(presignedUrl);

            // 트랜잭션 동기화 활성화 (실제 @Transactional이 없으므로 수동 init)
            TransactionSynchronizationManager.initSynchronization();

            // When
            KopicEvaluateResponse response = kopicEvaluateService.evaluate(member, request, audioData, contentType);

            // Then - 동기 응답 검증
            assertThat(response.getKopicReportId()).isEqualTo(1001L);
            assertThat(response.getStatus()).isEqualTo("PROCESSING");

            // S3 업로드 호출 검증
            verify(s3Service).uploadFile(s3Key, audioData, contentType);

            // Gemini는 afterCommit 콜백에 등록만 됨 — 아직 호출 안 됐어야 함
            verify(geminiAnalysisService, never()).analyze(anyLong(), any(), any(), anyLong());

            // afterCommit 시뮬레이트 — 등록된 콜백 직접 실행
            List<TransactionSynchronization> syncs = TransactionSynchronizationManager.getSynchronizations();
            assertThat(syncs).hasSize(1);
            syncs.get(0).afterCommit();

            // 이제 Gemini 호출됐어야 함
            verify(geminiAnalysisService).analyze(1001L, "오늘 점심 메뉴는 뭐예요?", presignedUrl, 2001L);
        }

        @Test
        @DisplayName("실패: contentType이 null이면 기본값 audio/wav 사용")
        void evaluate_defaultContentType() {
            Member member = mock(Member.class);
            given(member.getMemberId()).willReturn(1L);

            Theme theme = mock(Theme.class);
            KopicSentence sentence = mock(KopicSentence.class);
            given(sentence.getKopicSentenceId()).willReturn(501L);
            given(sentence.getTheme()).willReturn(theme);
            given(sentence.getTextKo()).willReturn("text");

            KopicTotalReport totalReport = mock(KopicTotalReport.class);
            given(totalReport.getKopicTotalReportId()).willReturn(2001L);

            KopicEvaluateRequest request = mock(KopicEvaluateRequest.class);
            given(request.getKopicTotalReportId()).willReturn(2001L);
            given(request.getKopicSentenceId()).willReturn(501L);

            given(kopicTotalReportRepository.findById(2001L)).willReturn(Optional.of(totalReport));
            given(kopicSentenceRepository.findById(501L)).willReturn(Optional.of(sentence));
            given(kopicReportRepository.save(any(KopicReport.class))).willAnswer(inv -> inv.getArgument(0));
            given(s3Service.generateKopicKey(anyLong(), anyLong())).willReturn("kopic/1/501.wav");
            given(s3Service.generatePresignedUrlForDownload(any())).willReturn("https://...");

            TransactionSynchronizationManager.initSynchronization();

            kopicEvaluateService.evaluate(member, request, new byte[]{1}, null);

            verify(s3Service).uploadFile(any(), any(), eq("audio/wav"));
        }

        @Test
        @DisplayName("실패: 통합 리포트가 없으면 NOT_FOUND_KOPIC_TOTAL_REPORT")
        void evaluate_notFoundTotalReport() {
            Member member = mock(Member.class);
            KopicEvaluateRequest request = mock(KopicEvaluateRequest.class);
            given(request.getKopicTotalReportId()).willReturn(999L);
            given(kopicTotalReportRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> kopicEvaluateService.evaluate(member, request, new byte[]{1}, "audio/wav"))
                    .isInstanceOf(BusinessException.class);

            verify(s3Service, never()).uploadFile(any(), any(), any());
            verifyNoInteractions(geminiAnalysisService);
        }

        @Test
        @DisplayName("실패: 코픽 문장이 없으면 NOT_FOUND_KOPIC_SENTENCE, S3/Gemini 호출 안 됨")
        void evaluate_notFoundSentence() {
            Member member = mock(Member.class);
            KopicTotalReport totalReport = mock(KopicTotalReport.class);

            KopicEvaluateRequest request = mock(KopicEvaluateRequest.class);
            given(request.getKopicTotalReportId()).willReturn(2001L);
            given(request.getKopicSentenceId()).willReturn(999L);

            given(kopicTotalReportRepository.findById(2001L)).willReturn(Optional.of(totalReport));
            given(kopicSentenceRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> kopicEvaluateService.evaluate(member, request, new byte[]{1}, "audio/wav"))
                    .isInstanceOf(BusinessException.class);

            verify(s3Service, never()).uploadFile(any(), any(), any());
            verifyNoInteractions(geminiAnalysisService);
        }
    }

    @Nested
    @DisplayName("getReport - 코픽 리포트 조회")
    class GetReportTest {

        @Test
        @DisplayName("성공: PROCESSING 상태면 상태값만 반환한다")
        void getReport_processing() {
            KopicReport report = mock(KopicReport.class);
            given(report.getKopicReportId()).willReturn(1001L);
            given(report.getStatus()).willReturn(ReportStatus.PROCESSING);
            given(kopicReportRepository.findById(1001L)).willReturn(Optional.of(report));

            KopicReportResponse response = kopicEvaluateService.getReport(1001L);

            assertThat(response.getKopicReportId()).isEqualTo(1001L);
            assertThat(response.getStatus()).isEqualTo("PROCESSING");
            assertThat(response.getAccuracy()).isNull();
            assertThat(response.getDetailedAnalysis()).isNull();
        }

        @Test
        @DisplayName("성공: COMPLETED 상태면 전체 분석 결과를 반환한다")
        void getReport_completed() {
            Member member = mock(Member.class);
            given(member.getMemberId()).willReturn(1L);

            KopicSentence sentence = mock(KopicSentence.class);
            given(sentence.getKopicSentenceId()).willReturn(501L);
            given(sentence.getTextKo()).willReturn("오늘 점심 메뉴는 뭐예요?");

            KopicReport report = mock(KopicReport.class);
            given(report.getKopicReportId()).willReturn(1001L);
            given(report.getMember()).willReturn(member);
            given(report.getKopicSentence()).willReturn(sentence);
            given(report.getAccuracy()).willReturn(85);
            given(report.getIntonation()).willReturn(80);
            given(report.getStatus()).willReturn(ReportStatus.COMPLETED);
            given(report.getDetailedAnalysis())
                    .willReturn("{\"missed_point\":\"test\",\"correction\":\"test\",\"tip\":\"test\"}");
            given(kopicReportRepository.findById(1001L)).willReturn(Optional.of(report));

            KopicReportResponse response = kopicEvaluateService.getReport(1001L);

            assertThat(response.getKopicReportId()).isEqualTo(1001L);
            assertThat(response.getStatus()).isEqualTo("COMPLETED");
            assertThat(response.getAccuracy()).isEqualTo(85);
            assertThat(response.getIntonation()).isEqualTo(80);
            assertThat(response.getDetailedAnalysis()).isNotNull();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 리포트 ID로 조회하면 예외 발생")
        void getReport_notFound() {
            given(kopicReportRepository.findById(999L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> kopicEvaluateService.getReport(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
