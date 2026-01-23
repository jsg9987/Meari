package com.ssafy.meari.domain.kopic.entity;

import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "kopic_sentence", indexes = {
    @Index(name = "idx_kopic_sentence_theme_id", columnList = "theme_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class KopicSentence extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "kopic_sentence_id")
    private Long kopicSentenceId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false, foreignKey = @ForeignKey(name = "FK_theme_TO_kopic_sentence_1"))
    private Theme theme;

    @Column(name = "text_ko", nullable = false, length = 1000)
    private String textKo;

    @Column(name = "kopic_sentence_url", nullable = false, length = 2048)
    private String kopicSentenceUrl;
}
