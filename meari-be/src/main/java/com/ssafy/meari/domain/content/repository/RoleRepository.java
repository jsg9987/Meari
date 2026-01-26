package com.ssafy.meari.domain.content.repository;

import com.ssafy.meari.domain.content.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
}
