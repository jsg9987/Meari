package com.ssafy.meari.domain.kopic.repository;

import com.ssafy.meari.domain.kopic.entity.KopicSentence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KopicSentenceRepository extends JpaRepository<KopicSentence, Long> {

    List<KopicSentence> findByTheme_ThemeId(Long themeId);
}
