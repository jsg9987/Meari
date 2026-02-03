package com.ssafy.meari.domain.dashboard.entity;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "daily_record",
    uniqueConstraints = {
        @UniqueConstraint(name = "uk_member_date",
            columnNames = {"member_id", "record_date"})
    },
    indexes = {
        @Index(name = "idx_daily_record_member_date",
            columnList = "member_id, record_date DESC")
    }
)
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class DailyRecord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "daily_record_id")
    private Long dailyRecordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false,
        foreignKey = @ForeignKey(name = "FK_member_TO_daily_record_1"))
    private Member member;

    @Column(name = "record_date", nullable = false)
    private LocalDate recordDate;

    @Column(name = "completed_count", nullable = false)
    private Integer completedCount = 0;

    @Builder
    public DailyRecord(Member member, LocalDate recordDate) {
        this.member = member;
        this.recordDate = recordDate;
        this.completedCount = 0;
    }

    // 도메인 메서드: 학습 완료 추가 (Dirty Checking 활용)
    public void incrementCompletedCount() {
        this.completedCount++;
    }

    // 도메인 메서드: level 계산 (completedCount 그대로 사용)
    public int getLevel() {
        return this.completedCount;
    }

    // 도메인 메서드: 학습 완료 여부 확인
    public boolean hasAnyCompletion() {
        return this.completedCount > 0;
    }
}
