package com.ssafy.meari.domain.kopic.service;

import com.ssafy.meari.domain.kopic.dto.request.KopicEvaluateRequest;
import com.ssafy.meari.domain.kopic.dto.response.KopicEvaluateResponse;
import com.ssafy.meari.domain.kopic.dto.response.KopicReportResponse;
import com.ssafy.meari.domain.kopic.entity.KopicSentence;
import com.ssafy.meari.domain.kopic.repository.KopicSentenceRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.report.entity.KopicReport;
import com.ssafy.meari.domain.report.entity.ReportStatus;
import com.ssafy.meari.domain.report.repository.KopicReportRepository;
import com.ssafy.meari.domain.report.repository.KopicTotalReportRepository;
import com.ssafy.meari.domain.report.entity.KopicTotalReport;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import com.ssafy.meari.global.error.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

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

    @Nested
    @DisplayName("evaluate - 코픽 발화 분석 요청")
    class EvaluateTest {

        @Test
        @DisplayName("성공: PROCESSING 상태의 리포트를 생성하고 비동기 분석을 시작한다")
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
            given(totalReport.getKopicTotalReportId()).willReturn(1L);

            KopicEvaluateRequest request = mock(KopicEvaluateRequest.class);
            given(request.getKopicTotalReportId()).willReturn(1L);
            given(request.getKopicSentenceId()).willReturn(501L);

            byte[] audioData = "fake-audio-data".getBytes();

            given(kopicTotalReportRepository.findById(1L)).willReturn(Optional.of(totalReport));
            given(kopicSentenceRepository.findById(501L)).willReturn(Optional.of(sentence));
            given(kopicReportRepository.save(any(KopicReport.class))).willAnswer(invocation -> {
                KopicReport report = invocation.getArgument(0);
                ReflectionTestUtils.setField(report, "kopicReportId", 1001L);
                return report;
            });

            // When
            KopicEvaluateResponse response = kopicEvaluateService.evaluate(member, request, audioData);

            // Then
            assertThat(response.getKopicReportId()).isEqualTo(1001L);
            assertThat(response.getStatus()).isEqualTo("PROCESSING");
        }

        @Test
        @DisplayName("실패: 존재하지 않는 코픽 문장 ID로 요청하면 예외 발생")
        void evaluate_notFoundSentence() {
            // Given
            Member member = mock(Member.class);
            byte[] audioData = "fake-audio-data".getBytes();

            KopicTotalReport totalReport = mock(KopicTotalReport.class);

            KopicEvaluateRequest request = mock(KopicEvaluateRequest.class);
            given(request.getKopicTotalReportId()).willReturn(1L);
            given(request.getKopicSentenceId()).willReturn(999L);

            given(kopicTotalReportRepository.findById(1L)).willReturn(Optional.of(totalReport));
            given(kopicSentenceRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> kopicEvaluateService.evaluate(member, request, audioData))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("getReport - 코픽 리포트 조회")
    class GetReportTest {

        @Test
        @DisplayName("성공: PROCESSING 상태면 상태값만 반환한다")
        void getReport_processing() {
            // Given
            KopicReport report = mock(KopicReport.class);
            given(report.getKopicReportId()).willReturn(1001L);
            given(report.getStatus()).willReturn(ReportStatus.PROCESSING);
            given(kopicReportRepository.findById(1001L)).willReturn(Optional.of(report));

            // When
            KopicReportResponse response = kopicEvaluateService.getReport(1001L);

            // Then
            assertThat(response.getKopicReportId()).isEqualTo(1001L);
            assertThat(response.getStatus()).isEqualTo("PROCESSING");
            assertThat(response.getAccuracy()).isNull();
            assertThat(response.getDetailedAnalysis()).isNull();
        }

        @Test
        @DisplayName("성공: COMPLETED 상태면 전체 분석 결과를 반환한다")
        void getReport_completed() {
            // Given
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
            given(report.getStatus()).willReturn(ReportStatus.COMPLETED);
            given(report.getDetailedAnalysis()).willReturn("{\"missed_point\":\"test\",\"correction\":\"test\",\"tip\":\"test\"}");
            given(kopicReportRepository.findById(1001L)).willReturn(Optional.of(report));

            // When
            KopicReportResponse response = kopicEvaluateService.getReport(1001L);

            // Then
            assertThat(response.getKopicReportId()).isEqualTo(1001L);
            assertThat(response.getStatus()).isEqualTo("COMPLETED");
            assertThat(response.getAccuracy()).isEqualTo(85);
            assertThat(response.getDetailedAnalysis()).isNotNull();
        }

        @Test
        @DisplayName("실패: 존재하지 않는 리포트 ID로 조회하면 예외 발생")
        void getReport_notFound() {
            // Given
            given(kopicReportRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> kopicEvaluateService.getReport(999L))
                    .isInstanceOf(BusinessException.class);
        }
    }
}
