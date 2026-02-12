package com.ssafy.meari.domain.content.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Sentence;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {

    List<Sentence> findByContent_ContentId(Long contentId);
    List<Sentence> findByContent(Content content);

    @Query(value = "SELECT * FROM sentence ORDER BY RANDOM() LIMIT :count", nativeQuery = true)
    List<Sentence> findRandomSentences(@Param("count") int count);
}
