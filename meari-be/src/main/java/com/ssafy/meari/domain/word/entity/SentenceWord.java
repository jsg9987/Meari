package com.ssafy.meari.domain.word.entity;

import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "sentence_word", indexes = {
    @Index(name = "idx_sentence_word_sentence_id", columnList = "sentence_id"),
    @Index(name = "idx_sentence_word_word_id", columnList = "word_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class SentenceWord extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sentence_word_id")
    private Long sentenceWordId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "word_id", nullable = false, foreignKey = @ForeignKey(name = "FK_word_TO_sentence_word_1"))
    private Word word;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sentence_id", nullable = false, foreignKey = @ForeignKey(name = "FK_sentence_TO_sentence_word_1"))
    private Sentence sentence;

    @Column(name = "sequence", nullable = false)
    private Integer sequence;
}
