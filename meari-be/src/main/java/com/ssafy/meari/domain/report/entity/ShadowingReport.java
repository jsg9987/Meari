package com.ssafy.meari.domain.report.entity;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Role;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "shadowing_report", indexes = {
    @Index(name = "idx_shadowing_report_member_id", columnList = "member_id"),
    @Index(name = "idx_shadowing_report_room_id", columnList = "room_id"),
    @Index(name = "idx_member_isread", columnList = "member_id, is_read")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ShadowingReport extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "shadowing_report_id")
    private Long shadowingReportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "FK_member_TO_shadowing_report_1"))
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false, foreignKey = @ForeignKey(name = "FK_room_TO_shadowing_report_1"))
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "FK_role_TO_shadowing_report_1"))
    private Role role;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false, foreignKey = @ForeignKey(name = "FK_content_TO_shadowing_report_1"))
    private Content content;

    @Column(name = "round", nullable = false)
    private Integer round;

    @Column(name = "accuracy")
    private Integer accuracy;

    @Column(name = "intonation")
    private Integer intonation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detailed_analysis", columnDefinition = "jsonb")
    private String detailedAnalysis;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private ReportStatus status;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @Builder
    public ShadowingReport(Member member, Room room, Role role, Content content, Integer round) {
        this.member = member;
        this.room = room;
        this.role = role;
        this.content = content;
        this.round = round;
        this.status = ReportStatus.PROCESSING;
    }

    // 도메인 메서드: 분석 결과 업데이트
    public void updateAnalysisResult(Integer accuracy, Integer intonation, String detailedAnalysis) {
        this.accuracy = accuracy;
        this.intonation = intonation;
        this.detailedAnalysis = detailedAnalysis;
        this.status = ReportStatus.COMPLETED;
    }

    // 도메인 메서드: 분석 실패 처리
    public void markAsFailed() {
        this.status = ReportStatus.FAILED;
    }

    // 도메인 메서드: 분석 완료 여부
    public boolean isCompleted() {
        return this.status == ReportStatus.COMPLETED;
    }

    // 도메인 메서드: 리포트 읽음 처리
    public void markAsRead() {
        this.isRead = true;
    }
}
