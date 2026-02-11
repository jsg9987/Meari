package com.ssafy.meari.domain.content.repository;

import com.ssafy.meari.domain.content.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * 특정 콘텐츠의 역할(캐릭터) 목록 조회
     */
    List<Role> findByContent_ContentId(Long contentId);
}
