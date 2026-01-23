package com.ssafy.meari.domain.word.entity;

import com.ssafy.meari.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "word")
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
}
