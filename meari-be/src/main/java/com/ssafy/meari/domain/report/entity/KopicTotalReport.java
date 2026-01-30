package com.ssafy.meari.domain.report.entity;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "kopic_total_report", indexes = {
    @Index(name = "idx_kopic_total_report_member_id", columnList = "member_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class KopicTotalReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kopic_total_report_id")
    private Long kopicTotalReportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "FK_member_TO_kopic_total_report_1"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false, foreignKey = @ForeignKey(name = "FK_theme_TO_kopic_total_report_1"))
    private Theme theme;

    @Column(name = "avg_accuracy")
    private Integer avgAccuracy;

    @Column(name = "avg_intonation")
    private Integer avgIntonation;

    @Column(name = "total_score")
    private Integer totalScore;

    @Column(name = "sentence_count")
    private Integer sentenceCount;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_data", columnDefinition = "jsonb")
    private String reportData;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReportStatus status;

    @Builder
    public KopicTotalReport(Member member, Theme theme, ReportStatus status) {
        this.member = member;
        this.theme = theme;
        this.status = status;
        this.reportData = "[]";
        this.sentenceCount = 0;
        this.avgAccuracy = 0;
        this.avgIntonation = 0;
        this.totalScore = 0;
    }

    public void updateAggregation(Integer avgAccuracy, Integer avgIntonation, Integer totalScore,
                                  Integer sentenceCount, String reportData) {
        this.avgAccuracy = avgAccuracy;
        this.avgIntonation = avgIntonation;
        this.totalScore = totalScore;
        this.sentenceCount = sentenceCount;
        this.reportData = reportData;
        this.status = ReportStatus.COMPLETED;
    }
}
