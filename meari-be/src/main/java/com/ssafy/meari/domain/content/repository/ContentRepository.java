package com.ssafy.meari.domain.content.repository;

import com.ssafy.meari.domain.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContentRepository extends JpaRepository<Content, Long> {

    /**
     * 특정 테마의 콘텐츠 목록 조회
     */
    List<Content> findByTheme_ThemeId(Long themeId);
}
