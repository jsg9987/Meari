package com.ssafy.meari.domain.content.entity;

import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "content", indexes = {
    @Index(name = "idx_content_theme_id", columnList = "theme_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Content extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "content_id")
    private Long contentId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false, foreignKey = @ForeignKey(name = "FK_theme_TO_content_1"))
    private Theme theme;

    @Column(name = "title", nullable = false, length = 100)
    private String title;

    @Column(name = "video_url", nullable = false, length = 2048)
    private String videoUrl;

    @Column(name = "thumbnail_url", length = 2048)
    private String thumbnailUrl;

    @Column(name = "max_people", nullable = false)
    private Integer maxPeople;

    @Column(name = "total_duration", nullable = false, precision = 10, scale = 3)
    private BigDecimal totalDuration;
}
