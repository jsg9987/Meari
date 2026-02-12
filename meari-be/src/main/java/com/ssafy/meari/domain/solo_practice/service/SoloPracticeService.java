package com.ssafy.meari.domain.solo_practice.service;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Role;
import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.content.repository.SentenceRepository;
import com.ssafy.meari.domain.solo_practice.dto.response.SoloPracticeContentResponse;
import com.ssafy.meari.domain.solo_practice.dto.response.SoloPracticeRoleResponse;
import com.ssafy.meari.domain.solo_practice.dto.response.SoloPracticeSentenceResponse;
import com.ssafy.meari.domain.solo_practice.dto.response.SoloPracticeStartResponse;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SoloPracticeService {

    private final ContentRepository contentRepository;
    private final RoleRepository roleRepository;
    private final SentenceRepository sentenceRepository;

    /**
     * 혼자연습 시작
     * - Content 검증
     * - Role 검증
     * - Role이 Content에 속하는지 검증
     * - Sentence 조회
     * - 응답 데이터 조립
     */
    public SoloPracticeStartResponse startPractice(Long contentId, Long roleId) {
        log.info("혼자연습 시작: contentId={}, roleId={}", contentId, roleId);

        // 1. Content 존재 확인
        Content content = contentRepository.findById(contentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONTENT));
        log.debug("Content 조회 완료: contentId={}, title={}", contentId, content.getTitle());

        // 2. Role 존재 확인
        Role selectedRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_ROLE));
        log.debug("Role 조회 완료: roleId={}, name={}", roleId, selectedRole.getName());

        // 3. Role이 Content에 속하는지 확인
        if (!selectedRole.getContent().getContentId().equals(contentId)) {
            log.warn("Role이 Content에 속하지 않음: contentId={}, roleId={}", contentId, roleId);
            throw new BusinessException(ErrorCode.INVALID_ROLE_FOR_CONTENT);
        }

        // 4. Content의 모든 Role 조회
        List<Role> allRoles = roleRepository.findByContent_ContentId(contentId);
        log.debug("모든 Role 조회 완료: contentId={}, roleCount={}", contentId, allRoles.size());

        // 5. Sentence 조회 (sequence 순서)
        List<Sentence> sentences = new ArrayList<>(sentenceRepository.findByContent_ContentId(contentId));
        sentences.sort(Comparator.comparingInt(Sentence::getSequence));
        log.debug("Sentence 조회 완료: contentId={}, sentenceCount={}", contentId, sentences.size());

        // 6. 응답 데이터 조립
        return buildStartResponse(content, selectedRole, allRoles, sentences);
    }

    /**
     * 시작 응답 데이터 조립
     */
    private SoloPracticeStartResponse buildStartResponse(
            Content content,
            Role selectedRole,
            List<Role> allRoles,
            List<Sentence> sentences
    ) {
        SoloPracticeContentResponse contentResponse = SoloPracticeContentResponse.from(content);
        SoloPracticeRoleResponse selectedRoleResponse = SoloPracticeRoleResponse.from(selectedRole);
        List<SoloPracticeRoleResponse> allRolesResponse = allRoles.stream()
                .map(SoloPracticeRoleResponse::from)
                .collect(Collectors.toList());
        List<SoloPracticeSentenceResponse> sentencesResponse = sentences.stream()
                .map(SoloPracticeSentenceResponse::from)
                .collect(Collectors.toList());

        return SoloPracticeStartResponse.builder()
                .content(contentResponse)
                .selectedRole(selectedRoleResponse)
                .allRoles(allRolesResponse)
                .sentences(sentencesResponse)
                .build();
    }
}
