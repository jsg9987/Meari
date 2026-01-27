package com.ssafy.meari.domain.room.repository;

import com.ssafy.meari.domain.room.entity.MemberRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MemberRoomRepository extends JpaRepository<MemberRoom, Long> {

    // 특정 방의 참여자 수 조회
    long countByRoom_RoomId(Long roomId);

    // 특정 방에 특정 회원이 참여 중인지 확인
    boolean existsByRoom_RoomIdAndMember_MemberId(Long roomId, Long memberId);

    // 특정 방의 특정 회원 참여 정보 조회
    Optional<MemberRoom> findByRoom_RoomIdAndMember_MemberId(Long roomId, Long memberId);

    // 특정 방의 참여자 목록 조회 (회원 정보 fetch join)
    @Query("SELECT mr FROM MemberRoom mr " +
           "JOIN FETCH mr.member m " +
           "WHERE mr.room.roomId = :roomId " +
           "ORDER BY mr.createdAt ASC")
    List<MemberRoom> findByRoomIdWithMember(@Param("roomId") Long roomId);

    // 특정 방의 가장 먼저 입장한 참여자 조회 (방장 위임용)
    @Query("SELECT mr FROM MemberRoom mr " +
           "JOIN FETCH mr.member m " +
           "WHERE mr.room.roomId = :roomId " +
           "ORDER BY mr.createdAt ASC " +
           "LIMIT 1")
    Optional<MemberRoom> findFirstByRoomIdOrderByCreatedAtAsc(@Param("roomId") Long roomId);

    // 특정 회원이 참여 중인 방 목록 조회
    @Query("SELECT mr FROM MemberRoom mr " +
           "JOIN FETCH mr.room r " +
           "WHERE mr.member.memberId = :memberId")
    List<MemberRoom> findByMemberIdWithRoom(@Param("memberId") Long memberId);

    // 특정 방의 모든 참여 정보 삭제
    void deleteAllByRoom_RoomId(Long roomId);
}
