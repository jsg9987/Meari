package com.ssafy.meari.domain.content.repository;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {

    List<Sentence> findByContent_ContentId(Long contentId);

    List<Sentence> findByContent(Content content);
}
