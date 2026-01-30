package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.room.entity.GamePhase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 방 세션 관리 서비스
 * - 참여자 목록
 * - 준비 상태
 * - 역할 선점
 * - 콘텐츠 선택
 * - 진행 단계
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RoomSessionService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final long SESSION_TTL_HOURS = 24;

    // Redis Key 패턴
    private static final String KEY_MEMBERS = "room:%d:members";
    private static final String KEY_READY = "room:%d:ready";
    private static final String KEY_ROLES = "room:%d:roles";
    private static final String KEY_ROLES_CONFIRMED = "room:%d:roles_confirmed";
    private static final String KEY_CONTENT = "room:%d:content_id";
    private static final String KEY_PHASE = "room:%d:phase";
    private static final String KEY_DISCONNECTED = "room:%d:disconnected";
    private static final String KEY_MEMBER_ROOM = "member:%d:roomId";
    private static final String KEY_ROUND_START_TIME = "room:%d:round_start_time";
    private static final String KEY_MEMBER_RECORDINGS = "room:%d:round:%d:member:%d:recordings";
    private static final String KEY_MEMBER_TOTAL_SENTENCES = "room:%d:round:%d:member:%d:total_sentences";
    private static final String KEY_ROUND_TIMEOUT = "room:%d:round:%d:timeout";
    private static final String KEY_ROUND_COMPLETED = "room:%d:round:%d:completed";

    // === 참여자 관리 ===

    /**
     * 참여자 추가
     */
    public void addMember(Long roomId, Long memberId) {
        String key = String.format(KEY_MEMBERS, roomId); // Redis key 생성
        redisTemplate.opsForSet().add(key, memberId.toString()); // SADD 명령어: Set 구조에 memberId 추가
        setExpire(key); // key 만료시간 설정
        log.info("방 {} 참여자 추가: memberId={}", roomId, memberId);
    }

    /**
     * 참여자 제거
     */
    public void removeMember(Long roomId, Long memberId) {
        String key = String.format(KEY_MEMBERS, roomId);
        redisTemplate.opsForSet().remove(key, memberId.toString()); // SREM: Set에서 특정 참여자 삭제

        // 연관된 세션 데이터(준비 상태, 역할) 순차적 제거
        removeReady(roomId, memberId); // 준비 상태도 제거
        releaseRoleByMember(roomId, memberId); // 역할 선점도 해제

        log.info("방 {} 참여자 제거: memberId={}", roomId, memberId);
    }

    /**
     * 참여자 목록 조회
     */
    public Set<String> getMembers(Long roomId) {
        String key = String.format(KEY_MEMBERS, roomId);
        return redisTemplate.opsForSet().members(key);
    }

    /**
     * 참여자 수 조회
     */
    public long getMemberCount(Long roomId) {
        String key = String.format(KEY_MEMBERS, roomId);
        Long size = redisTemplate.opsForSet().size(key);
        return size != null ? size : 0;
    }

    /**
     * 참여 여부 확인
     */
    public boolean isMember(Long roomId, Long memberId) {
        String key = String.format(KEY_MEMBERS, roomId); // Redis key 생성
        Boolean isMember = redisTemplate.opsForSet().isMember(key, memberId.toString()); // SISMEMBER: 존재 여부 확인 (O(1))
        return Boolean.TRUE.equals(isMember);
    }

    // === 준비 상태 관리 ===

    /**
     * 준비 상태 설정
     */
    public void setReady(Long roomId, Long memberId, boolean ready) {
        String key = String.format(KEY_READY, roomId);
        // HSET: Hash 구조에 멤버별 준비 상태 기록 (field: memberId, value: true/false)
        redisTemplate.opsForHash().put(key, memberId.toString(), String.valueOf(ready));
        setExpire(key);
        log.info("방 {} 준비 상태 변경: memberId={}, ready={}", roomId, memberId, ready);
    }

    /**
     * 준비 상태 조회
     */
    public boolean isReady(Long roomId, Long memberId) {
        String key = String.format(KEY_READY, roomId);
        Object value = redisTemplate.opsForHash().get(key, memberId.toString());
        return "true".equals(value);
    }

    /**
     * 준비 상태 제거
     */
    public void removeReady(Long roomId, Long memberId) {
        String key = String.format(KEY_READY, roomId);
        redisTemplate.opsForHash().delete(key, memberId.toString());
    }

    /**
     * 전체 준비 상태 조회
     */
    public Map<Long, Boolean> getAllReadyStatus(Long roomId) {
        String key = String.format(KEY_READY, roomId);

        // 1. HGETALL: Hash 구조의 모든 필드(memberId)와 값(ready 여부)을 일괄 조회
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key); // 방 Id로

        // 2. 응답 가공: Redis의 String 데이터를 자바의 Long(ID)과 Boolean(상태) 타입으로 변환
        Map<Long, Boolean> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(Long.parseLong(k.toString()), "true".equals(v)));
        return result;
    }

    /**
     * 모든 참여자가 준비 완료인지 확인
     */
    public boolean isAllReady(Long roomId) {
        Set<String> members = getMembers(roomId); // 1. 현재 참여자 목록(Set) 조회
        if (members == null || members.isEmpty()) {
            return false;
        }

        Map<Long, Boolean> readyStatus = getAllReadyStatus(roomId); // 2. 전체 준비 현황(Hash) 조회

        // 3. 모두 준비 완료인지 체크
        for (String memberId : members) {
            if (!Boolean.TRUE.equals(readyStatus.get(Long.parseLong(memberId)))) {
                return false;
            }
        }
        return true;
    }

    // === 역할 선점 관리 ===

    /**
     * 역할 선점 시도 (원자성 보장)
     * @return true: 선점 성공, false: 이미 선점됨
     */
    public boolean tryAssignRole(Long roomId, Long roleId, Long memberId) {
        String key = String.format(KEY_ROLES, roomId);

        // 1. 이미 다른 역할을 선점했다면 기존 역할 먼저 해제
        releaseRoleByMember(roomId, memberId);

        // 2. HSETNX: 해당 역할(Field)이 비어있을 때만 원자적으로 memberId 기록
        Boolean success = redisTemplate.opsForHash().putIfAbsent(key, roleId.toString(), memberId.toString());
        if (Boolean.TRUE.equals(success)) {
            setExpire(key);
            log.info("방 {} 역할 선점 성공: roleId={}, memberId={}", roomId, roleId, memberId);
            return true;
        }

        log.info("방 {} 역할 선점 실패 (이미 선점됨): roleId={}", roomId, roleId);
        return false;
    }

    /**
     * 역할 선점 해제
     */
    public void releaseRole(Long roomId, Long roleId) {
        String key = String.format(KEY_ROLES, roomId);
        redisTemplate.opsForHash().delete(key, roleId.toString());
        log.info("방 {} 역할 해제: roleId={}", roomId, roleId);
    }

    /**
     * 모든 역할 초기화
     */
    public void clearRoles(Long roomId) {
        String key = String.format(KEY_ROLES, roomId);
        redisTemplate.delete(key);
        log.info("방 {} 모든 역할 초기화", roomId);
    }

    /**
     * 역할 직접 할당 (확정 시 사용)
     */
    public void assignRole(Long roomId, Long roleId, Long memberId) {
        String key = String.format(KEY_ROLES, roomId);
        redisTemplate.opsForHash().put(key, roleId.toString(), memberId.toString());
        setExpire(key);
        log.info("방 {} 역할 할당: roleId={}, memberId={}", roomId, roleId, memberId);
    }

    /**
     * 특정 멤버가 선점한 역할 해제
     */
    public void releaseRoleByMember(Long roomId, Long memberId) {
        String key = String.format(KEY_ROLES, roomId);
        Map<Object, Object> roles = redisTemplate.opsForHash().entries(key);

        // 특정 멤버가 선점한 역할 있다면 제거
        for (Map.Entry<Object, Object> entry : roles.entrySet()) {
            if (memberId.toString().equals(entry.getValue())) {
                redisTemplate.opsForHash().delete(key, entry.getKey());
                log.info("방 {} 멤버의 역할 해제: memberId={}, roleId={}", roomId, memberId, entry.getKey());
                break;
            }
        }
    }

    /**
     * 역할 선점자 조회
     * @return memberId 또는 null (선점자 없음)
     */
    public Long getRoleOwner(Long roomId, Long roleId) {
        String key = String.format(KEY_ROLES, roomId);
        Object value = redisTemplate.opsForHash().get(key, roleId.toString());
        if (value != null && !"SYSTEM".equals(value)) {
            return Long.parseLong(value.toString());
        }
        return null;
    }

    /**
     * 역할을 시스템으로 지정 (탈주 시)
     */
    public void assignRoleToSystem(Long roomId, Long roleId) {
        String key = String.format(KEY_ROLES, roomId);
        redisTemplate.opsForHash().put(key, roleId.toString(), "SYSTEM");
        log.info("방 {} 역할 시스템 지정: roleId={}", roomId, roleId);
    }

    /**
     * 전체 역할 선점 현황 조회
     * @return Map<roleId, memberId or "SYSTEM">
     */
    public Map<Long, String> getAllRoles(Long roomId) {
        String key = String.format(KEY_ROLES, roomId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
        Map<Long, String> result = new HashMap<>();
        entries.forEach((k, v) -> result.put(Long.parseLong(k.toString()), v.toString()));
        return result;
    }

    /**
     * 특정 멤버가 선점한 역할 ID 조회
     */
    public Long getMemberRole(Long roomId, Long memberId) {
        Map<Long, String> roles = getAllRoles(roomId);
        for (Map.Entry<Long, String> entry : roles.entrySet()) {
            if (memberId.toString().equals(entry.getValue())) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * 역할 확정 상태 설정
     */
    public void setRolesConfirmed(Long roomId, boolean confirmed) {
        String key = String.format(KEY_ROLES_CONFIRMED, roomId);
        redisTemplate.opsForValue().set(key, String.valueOf(confirmed));
        setExpire(key);
        log.info("방 {} 역할 확정 상태 변경: confirmed={}", roomId, confirmed);
    }

    /**
     * 역할 확정 여부 확인
     */
    public boolean isRolesConfirmed(Long roomId) {
        String key = String.format(KEY_ROLES_CONFIRMED, roomId);
        String value = redisTemplate.opsForValue().get(key);
        return "true".equals(value);
    }

    // === 콘텐츠 관리 ===

    /**
     * 현재 선택된 콘텐츠 설정
     */
    public void setContent(Long roomId, Long contentId) {
        String key = String.format(KEY_CONTENT, roomId);
        redisTemplate.opsForValue().set(key, contentId.toString());
        setExpire(key);
        log.info("방 {} 콘텐츠 설정: contentId={}", roomId, contentId);
    }

    /**
     * 현재 선택된 콘텐츠 조회
     */
    public Long getContent(Long roomId) {
        String key = String.format(KEY_CONTENT, roomId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : null;
    }

    /**
     * 현재 선택된 콘텐츠 ID 조회 (별칭)
     */
    public Long getContentId(Long roomId) {
        return getContent(roomId);
    }

    // === 진행 단계 관리 ===

    /**
     * 진행 단계 설정
     */
    public void setPhase(Long roomId, GamePhase phase) {
        String key = String.format(KEY_PHASE, roomId);
        redisTemplate.opsForValue().set(key, phase.name());
        setExpire(key);
        log.info("방 {} 진행 단계 변경: phase={}", roomId, phase);
    }

    /**
     * 진행 단계 조회
     */
    public GamePhase getPhase(Long roomId) {
        String key = String.format(KEY_PHASE, roomId);
        String value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return null;
        }
        return com.ssafy.meari.domain.room.entity.GamePhase.valueOf(value);
    }

    // === 연결 끊김 관리 (Grace Period) ===

    /**
     * 연결 끊김 마킹
     */
    public void markDisconnected(Long roomId, Long memberId) {
        String key = String.format(KEY_DISCONNECTED, roomId);
        redisTemplate.opsForHash().put(key, memberId.toString(), String.valueOf(System.currentTimeMillis()));
        setExpire(key);
        log.info("방 {} 연결 끊김 마킹: memberId={}", roomId, memberId);
    }

    /**
     * 연결 끊김 해제 (재연결 시)
     */
    public void clearDisconnected(Long roomId, Long memberId) {
        String key = String.format(KEY_DISCONNECTED, roomId);
        redisTemplate.opsForHash().delete(key, memberId.toString());
        log.info("방 {} 연결 복구: memberId={}", roomId, memberId);
    }

    /**
     * 연결 끊김 여부 확인
     */
    public boolean isDisconnected(Long roomId, Long memberId) {
        String key = String.format(KEY_DISCONNECTED, roomId);
        return redisTemplate.opsForHash().hasKey(key, memberId.toString());
    }

    // === 멤버→방 매핑 관리 (WebSocket 연결 해제 시 roomId 조회용) ===

    /**
     * 멤버가 참여 중인 방 ID 저장
     */
    public void setMemberRoom(Long memberId, Long roomId) {
        String key = String.format(KEY_MEMBER_ROOM, memberId);
        redisTemplate.opsForValue().set(key, roomId.toString());
        setExpire(key);
    }

    /**
     * 멤버가 참여 중인 방 ID 조회
     */
    public Long getMemberRoom(Long memberId) {
        String key = String.format(KEY_MEMBER_ROOM, memberId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : null;
    }

    /**
     * 멤버→방 매핑 제거
     */
    public void clearMemberRoom(Long memberId) {
        String key = String.format(KEY_MEMBER_ROOM, memberId);
        redisTemplate.delete(key);
    }

    // === Round 시작 시각 관리 ===

    /**
     * Round 시작 시각 저장 (영상 동기화용)
     */
    public void setRoundStartTime(Long roomId, Long startTimeEpochMillis) {
        String key = String.format(KEY_ROUND_START_TIME, roomId);
        redisTemplate.opsForValue().set(key, startTimeEpochMillis.toString());
        setExpire(key);
        log.debug("방 {} Round 시작 시각 저장: {}", roomId, startTimeEpochMillis);
    }

    /**
     * Round 시작 시각 조회
     */
    public Long getRoundStartTime(Long roomId) {
        String key = String.format(KEY_ROUND_START_TIME, roomId);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : null;
    }

    // === 문장별 녹음 완료 추적 ===

    /**
     * 멤버의 예상 문장수 저장
     */
    public void setMemberTotalSentences(Long roomId, Integer round, Long memberId, int totalSentences) {
        String key = String.format(KEY_MEMBER_TOTAL_SENTENCES, roomId, round, memberId);
        redisTemplate.opsForValue().set(key, String.valueOf(totalSentences));
        setExpire(key);
        log.debug("방 {} Round {} 멤버 {} 예상 문장수 저장: {}", roomId, round, memberId, totalSentences);
    }

    /**
     * 멤버의 문장 녹음 완료 마킹
     * @return true: 새로운 문장 완료, false: 이미 완료된 문장
     */
    public boolean markRecordingComplete(Long roomId, Integer round, Long memberId, Long sentenceId) {
        String key = String.format(KEY_MEMBER_RECORDINGS, roomId, round, memberId);
        Long added = redisTemplate.opsForSet().add(key, sentenceId.toString());
        if (added != null && added > 0) {
            setExpire(key);
            log.debug("방 {} Round {} 멤버 {} 문장 {} 녹음 완료 마킹", roomId, round, memberId, sentenceId);
            return true;
        }
        return false;
    }

    /**
     * 특정 멤버의 모든 문장 녹음이 완료되었는지 확인
     */
    public boolean isMemberRecordingsComplete(Long roomId, Integer round, Long memberId) {
        // 예상 문장수 조회
        String totalKey = String.format(KEY_MEMBER_TOTAL_SENTENCES, roomId, round, memberId);
        String totalValue = redisTemplate.opsForValue().get(totalKey);
        if (totalValue == null) {
            return false;
        }
        int totalSentences = Integer.parseInt(totalValue);

        // 완료된 문장수 조회
        String recordingsKey = String.format(KEY_MEMBER_RECORDINGS, roomId, round, memberId);
        Long completedCount = redisTemplate.opsForSet().size(recordingsKey);
        if (completedCount == null || completedCount < totalSentences) {
            return false;
        }

        log.debug("멤버 {} 모든 녹음 완료: {}/{}", memberId, completedCount, totalSentences);
        return true;
    }

    /**
     * 모든 멤버의 모든 문장 녹음이 완료되었는지 확인
     */
    public boolean isAllRecordingsComplete(Long roomId, Integer round) {
        Set<String> members = getMembers(roomId);
        if (members == null || members.isEmpty()) {
            return false;
        }

        for (String memberIdStr : members) {
            Long memberId = Long.parseLong(memberIdStr);
            if (!isMemberRecordingsComplete(roomId, round, memberId)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Round 관련 녹음 추적 데이터 초기화
     */
    public void clearRoundRecordings(Long roomId, Integer round) {
        Set<String> members = getMembers(roomId);
        if (members != null) {
            for (String memberIdStr : members) {
                Long memberId = Long.parseLong(memberIdStr);
                redisTemplate.delete(String.format(KEY_MEMBER_RECORDINGS, roomId, round, memberId));
                redisTemplate.delete(String.format(KEY_MEMBER_TOTAL_SENTENCES, roomId, round, memberId));
            }
        }
        redisTemplate.delete(String.format(KEY_ROUND_START_TIME, roomId));
        log.debug("방 {} Round {} 녹음 추적 데이터 초기화", roomId, round);
    }

    // === 세션 정리 ===

    /**
     * 방 세션 전체 삭제
     */
    public void clearRoomSession(Long roomId) {
        redisTemplate.delete(String.format(KEY_MEMBERS, roomId));
        redisTemplate.delete(String.format(KEY_READY, roomId));
        redisTemplate.delete(String.format(KEY_ROLES, roomId));
        redisTemplate.delete(String.format(KEY_ROLES_CONFIRMED, roomId));
        redisTemplate.delete(String.format(KEY_CONTENT, roomId));
        redisTemplate.delete(String.format(KEY_PHASE, roomId));
        redisTemplate.delete(String.format(KEY_DISCONNECTED, roomId));
        log.info("방 {} 세션 전체 삭제", roomId);
    }

    /**
     * 게임 상태만 초기화 (참여자 목록은 유지)
     * Round 종료 후 준비 단계로 복귀할 때 사용
     * WAITING 상태에서는 GamePhase가 없으므로 삭제
     */
    public void resetGameState(Long roomId) {
        redisTemplate.delete(String.format(KEY_READY, roomId));
        redisTemplate.delete(String.format(KEY_ROLES, roomId));
        redisTemplate.delete(String.format(KEY_ROLES_CONFIRMED, roomId));
        redisTemplate.delete(String.format(KEY_CONTENT, roomId));
        redisTemplate.delete(String.format(KEY_PHASE, roomId));
        redisTemplate.delete(String.format(KEY_DISCONNECTED, roomId));
        log.info("방 {} 게임 상태 초기화 (준비 단계로 복귀, phase 삭제)", roomId);
    }

    // === 타임아웃 관리 ===

    /**
     * 라운드 타임아웃 시간 설정
     */
    public void setRoundTimeout(Long roomId, Integer round, Long timeoutMillis) {
        String key = String.format(KEY_ROUND_TIMEOUT, roomId, round);
        redisTemplate.opsForValue().set(key, String.valueOf(timeoutMillis));
        setExpire(key);
        log.debug("라운드 타임아웃 설정: roomId={}, round={}, timeout={}", roomId, round, timeoutMillis);
    }

    /**
     * 라운드 타임아웃 시간 조회
     */
    public Long getRoundTimeout(Long roomId, Integer round) {
        String key = String.format(KEY_ROUND_TIMEOUT, roomId, round);
        String value = redisTemplate.opsForValue().get(key);
        return value != null ? Long.parseLong(value) : null;
    }

    /**
     * 라운드 완료 플래그 설정
     */
    public void markRoundCompleted(Long roomId, Integer round) {
        String key = String.format(KEY_ROUND_COMPLETED, roomId, round);
        redisTemplate.opsForValue().set(key, "true");
        setExpire(key);
        log.debug("라운드 완료 마킹: roomId={}, round={}", roomId, round);
    }

    /**
     * 라운드가 완료되었는지 확인
     */
    public boolean isRoundCompleted(Long roomId, Integer round) {
        String key = String.format(KEY_ROUND_COMPLETED, roomId, round);
        String value = redisTemplate.opsForValue().get(key);
        return "true".equals(value);
    }

    // === 유틸리티 ===

    // key의 만료 시간 설정
    private void setExpire(String key) {
        redisTemplate.expire(key, SESSION_TTL_HOURS, TimeUnit.HOURS);
    }
}
