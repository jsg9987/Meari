package com.ssafy.meari.domain.content.repository;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {

    List<Sentence> findByContent(Content content);

    List<Sentence> findByContent_ContentId(Long contentId);

    @Query(value = "SELECT * FROM sentence ORDER BY RAND() LIMIT :count", nativeQuery = true)
    List<Sentence> findRandomSentences(@Param("count") int count);
}
