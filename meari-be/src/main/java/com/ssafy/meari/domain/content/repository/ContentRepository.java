package com.ssafy.meari.domain.content.repository;

import com.ssafy.meari.domain.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ContentRepository extends JpaRepository<Content, Long> {
}
