package com.ssafy.meari.domain.content.entity;

import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "sentence", indexes = {
    @Index(name = "idx_sentence_content_id", columnList = "content_id"),
    @Index(name = "idx_sentence_role_id", columnList = "role_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Sentence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sentence_id")
    private Long sentenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "content_id", nullable = false, foreignKey = @ForeignKey(name = "FK_content_TO_sentence_1"))
    private Content content;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id", nullable = false, foreignKey = @ForeignKey(name = "FK_role_TO_sentence_1"))
    private Role role;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;

    @Column(name = "start_time", nullable = false, precision = 10, scale = 3)
    private BigDecimal startTime;

    @Column(name = "end_time", nullable = false, precision = 10, scale = 3)
    private BigDecimal endTime;

    @Column(name = "text_ko", nullable = false, length = 1000)
    private String textKo;

    @Column(name = "text_vn", nullable = false, length = 1000)
    private String textVn;
}
