package com.ssafy.meari.domain.content.repository;

import com.ssafy.meari.domain.content.entity.Sentence;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SentenceRepository extends JpaRepository<Sentence, Long> {
}
