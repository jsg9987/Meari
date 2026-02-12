package com.ssafy.meari.domain.word.entity;

import com.ssafy.meari.global.entity.BaseEntity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "word", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"word_kr", "homonym_kr_id"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Word extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "word_id")
    private Long wordId;

    @Column(name = "word_kr", nullable = false, length = 100)
    private String wordKr;

    @Column(name = "definition_kr", nullable = false, length = 500)
    private String definitionKr;

    @Column(name = "word_vn", nullable = false, length = 100)
    private String wordVn;

    @Column(name = "definition_vn", nullable = false, length = 500)
    private String definitionVn;

    @Column(name = "pronunciation_kr", nullable = false, length = 500)
    private String pronunciationKr;

    @Schema(description = "한국어 동음이의어 구분 인덱스")
    @Column(name = "homonym_kr_id", columnDefinition = "INTEGER DEFAULT 1")
    private Integer homonymKrId = 1;
}
