package com.ssafy.meari.domain.report.entity;

import com.ssafy.meari.domain.kopic.entity.KopicSentence;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "kopic_report", indexes = {
    @Index(name = "idx_kopic_report_member_id", columnList = "member_id"),
    @Index(name = "idx_kopic_report_theme_id", columnList = "theme_id"),
    @Index(name = "idx_kopic_report_kopic_sentence_id", columnList = "kopic_sentence_id"),
    @Index(name = "idx_kopic_report_total_report_id", columnList = "kopic_total_report_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KopicReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kopic_report_id")
    private Long kopicReportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "FK_member_TO_kopic_report_1"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false, foreignKey = @ForeignKey(name = "FK_theme_TO_kopic_report_1"))
    private Theme theme;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kopic_sentence_id", nullable = false, foreignKey = @ForeignKey(name = "FK_kopic_sentence_TO_kopic_report_1"))
    private KopicSentence kopicSentence;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kopic_total_report_id", nullable = false, foreignKey = @ForeignKey(name = "FK_kopic_total_report_TO_kopic_report_1"))
    private KopicTotalReport kopicTotalReport;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "accuracy")
    private Integer accuracy;

    @Column(name = "intonation")
    private Integer intonation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detailed_analysis", columnDefinition = "jsonb", nullable = true)
    private String detailedAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReportStatus status;

    @Builder
    public KopicReport(Member member, Theme theme, KopicSentence kopicSentence, KopicTotalReport kopicTotalReport, ReportStatus status) {
        this.member = member;
        this.theme = theme;
        this.kopicSentence = kopicSentence;
        this.kopicTotalReport = kopicTotalReport;
        this.status = status;
    }

    public void updateAnalysisResult(Integer accuracy, Integer intonation, String detailedAnalysis) {
        this.accuracy = accuracy;
        this.intonation = intonation;
        this.totalScore = (accuracy + intonation) / 2;
        this.detailedAnalysis = detailedAnalysis;
        this.status = ReportStatus.COMPLETED;
    }

    public void markAsFailed() {
        this.status = ReportStatus.FAILED;
    }
}
