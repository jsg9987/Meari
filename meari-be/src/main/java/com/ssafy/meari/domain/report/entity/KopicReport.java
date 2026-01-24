package com.ssafy.meari.domain.report.entity;

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
    @Index(name = "idx_kopic_report_theme_id", columnList = "theme_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
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

    @Column(name = "total_score")
    private Integer totalScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detailed_analysis", nullable = false, columnDefinition = "jsonb")
    private String detailedAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private ReportStatus status;
}
