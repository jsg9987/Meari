package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.room.dto.request.RoomCreateRequest;
import com.ssafy.meari.domain.room.dto.request.RoomEnterRequest;
import com.ssafy.meari.domain.room.dto.response.RoomDetailResponse;
import com.ssafy.meari.domain.room.dto.response.RoomListResponse;
import com.ssafy.meari.domain.room.dto.response.RoomResponse;
import com.ssafy.meari.global.common.CursorPageResponse;
import com.ssafy.meari.domain.room.entity.GamePhase;
import com.ssafy.meari.domain.room.entity.MemberRoom;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.entity.RoomStatus;
import com.ssafy.meari.domain.room.repository.MemberRoomRepository;
import com.ssafy.meari.domain.room.repository.RoomRepository;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService 단위 테스트")
class RoomServiceTest {

    @InjectMocks
    private RoomService roomService;

    // RoomServiceTest.java 상단 Mock 정의 구역에 추가
    @Mock
    private org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private MemberRoomRepository memberRoomRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ThemeRepository themeRepository;

    @Mock
    private RoomSessionService roomSessionService;

    @Mock
    private com.ssafy.meari.domain.content.repository.ContentRepository contentRepository;

    @Mock
    private com.ssafy.meari.domain.content.repository.RoleRepository roleRepository;

    @Mock
    private com.ssafy.meari.domain.content.repository.SentenceRepository sentenceRepository;

    @Mock
    private com.ssafy.meari.domain.report.repository.ShadowingReportRepository shadowingReportRepository;

    private Member testMember;
    private Theme testTheme;
    private Room testRoom;

    @BeforeEach
    void setUp() {
        testMember = Member.builder()
                .email("test@test.com")
                .password("password")
                .nickname("테스터")
                .build();
        ReflectionTestUtils.setField(testMember, "memberId", 1L);

        testTheme = Theme.builder()
                .name("일상 회화")
                .build();
        ReflectionTestUtils.setField(testTheme, "themeId", 1L);

        testRoom = Room.builder()
                .owner(testMember)
                .theme(testTheme)
                .title("테스트 방")
                .maxPeople(4)
                .password(null)
                .build();
        ReflectionTestUtils.setField(testRoom, "roomId", 1L);
    }

    @Nested
    @DisplayName("방 생성")
    class CreateRoom {

        @Test
        @DisplayName("성공 - 공개방 생성")
        void createRoom_Success_PublicRoom() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest();
            ReflectionTestUtils.setField(request, "title", "테스트 방");
            ReflectionTestUtils.setField(request, "themeId", 1L);
            ReflectionTestUtils.setField(request, "maxPeople", 4);
            ReflectionTestUtils.setField(request, "password", null);

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(themeRepository.findById(1L)).willReturn(Optional.of(testTheme));
            given(roomRepository.save(any(Room.class))).willReturn(testRoom);

            // When
            RoomResponse response = roomService.createRoom(request, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRoomId()).isEqualTo(1L);
            assertThat(response.getTitle()).isEqualTo("테스트 방");
            assertThat(response.getHasPassword()).isFalse();
            verify(memberRoomRepository).save(any(MemberRoom.class));
            verify(roomSessionService).addMember(1L, 1L);
        }

        @Test
        @DisplayName("성공 - 비밀방 생성")
        void createRoom_Success_PrivateRoom() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest();
            ReflectionTestUtils.setField(request, "title", "비밀 방");
            ReflectionTestUtils.setField(request, "themeId", 1L);
            ReflectionTestUtils.setField(request, "maxPeople", 2);
            ReflectionTestUtils.setField(request, "password", "1234");

            Room privateRoom = Room.builder()
                    .owner(testMember)
                    .theme(testTheme)
                    .title("비밀 방")
                    .maxPeople(2)
                    .password("1234")
                    .build();
            ReflectionTestUtils.setField(privateRoom, "roomId", 2L);

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(themeRepository.findById(1L)).willReturn(Optional.of(testTheme));
            given(roomRepository.save(any(Room.class))).willReturn(privateRoom);

            // When
            RoomResponse response = roomService.createRoom(request, 1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getHasPassword()).isTrue();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 회원")
        void createRoom_Fail_MemberNotFound() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest();
            ReflectionTestUtils.setField(request, "title", "테스트 방");
            ReflectionTestUtils.setField(request, "themeId", 1L);
            ReflectionTestUtils.setField(request, "maxPeople", 4);

            given(memberRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> roomService.createRoom(request, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_MEMBER);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 테마")
        void createRoom_Fail_ThemeNotFound() {
            // Given
            RoomCreateRequest request = new RoomCreateRequest();
            ReflectionTestUtils.setField(request, "title", "테스트 방");
            ReflectionTestUtils.setField(request, "themeId", 999L);
            ReflectionTestUtils.setField(request, "maxPeople", 4);

            given(memberRepository.findById(1L)).willReturn(Optional.of(testMember));
            given(themeRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> roomService.createRoom(request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_THEME);
        }
    }

    @Nested
    @DisplayName("방 목록 조회")
    class GetRoomList {

        @Test
        @DisplayName("성공 - 전체 방 목록 조회")
        void getRoomList_Success_AllRooms() {
            // Given
            given(roomRepository.findAllRoomsWithCursor(any(), any(), any()))
                    .willReturn(List.of(testRoom));
            given(memberRoomRepository.countByRoom_RoomId(1L)).willReturn(2L);

            // When
            CursorPageResponse<RoomListResponse> response = roomService.getRoomList(null, null, 10);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContents()).hasSize(1);
            assertThat(response.isHasNext()).isFalse();
        }

        @Test
        @DisplayName("성공 - 테마별 방 목록 조회")
        void getRoomList_Success_ByTheme() {
            // Given
            given(roomRepository.findRoomsWithCursor(eq(1L), any(), any(), any()))
                    .willReturn(List.of(testRoom));
            given(memberRoomRepository.countByRoom_RoomId(1L)).willReturn(1L);

            // When
            CursorPageResponse<RoomListResponse> response = roomService.getRoomList(1L, null, 10);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getContents()).hasSize(1);
        }

        @Test
        @DisplayName("성공 - 다음 페이지 존재")
        void getRoomList_Success_HasNextPage() {
            // Given
            Room room2 = Room.builder()
                    .owner(testMember)
                    .theme(testTheme)
                    .title("테스트 방 2")
                    .maxPeople(4)
                    .build();
            ReflectionTestUtils.setField(room2, "roomId", 2L);

            given(roomRepository.findAllRoomsWithCursor(any(), any(), any()))
                    .willReturn(List.of(testRoom, room2));
            given(memberRoomRepository.countByRoom_RoomId(anyLong())).willReturn(1L);

            // When
            CursorPageResponse<RoomListResponse> response = roomService.getRoomList(null, null, 1);

            // Then
            assertThat(response.isHasNext()).isTrue();
            assertThat(response.getNextCursor()).isEqualTo(1L);
            assertThat(response.getContents()).hasSize(1);
        }
    }

    @Nested
    @DisplayName("방 상세 조회")
    class GetRoomDetail {

        @Test
        @DisplayName("성공 - 방 상세 조회")
        void getRoomDetail_Success() {
            // Given
            MemberRoom memberRoom = MemberRoom.builder()
                    .room(testRoom)
                    .member(testMember)
                    .build();

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(memberRoomRepository.findByRoomIdWithMember(1L)).willReturn(List.of(memberRoom));
            given(roomSessionService.getAllReadyStatus(1L)).willReturn(Map.of(1L, false));
            given(roomSessionService.getAllRoles(1L)).willReturn(Collections.emptyMap());

            // When
            RoomDetailResponse response = roomService.getRoomDetail(1L);

            // Then
            assertThat(response).isNotNull();
            assertThat(response.getRoomId()).isEqualTo(1L);
            assertThat(response.getTitle()).isEqualTo("테스트 방");
            assertThat(response.getMembers()).hasSize(1);
            assertThat(response.getMembers().get(0).getIsOwner()).isTrue();
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 방")
        void getRoomDetail_Fail_RoomNotFound() {
            // Given
            given(roomRepository.findById(999L)).willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> roomService.getRoomDetail(999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_ROOM);
        }
    }

    @Nested
    @DisplayName("방 입장")
    class EnterRoom {

        private Member anotherMember;

        @BeforeEach
        void setUp() {
            anotherMember = Member.builder()
                    .email("another@test.com")
                    .password("password")
                    .nickname("다른사용자")
                    .build();
            ReflectionTestUtils.setField(anotherMember, "memberId", 2L);
        }

        @Test
        @DisplayName("성공 - 공개방 입장")
        void enterRoom_Success_PublicRoom() {
            // Given
            RoomEnterRequest request = new RoomEnterRequest();

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(memberRoomRepository.existsByRoom_RoomIdAndMember_MemberId(1L, 2L)).willReturn(false);
            given(memberRoomRepository.countByRoom_RoomId(1L)).willReturn(1L);
            given(memberRepository.findById(2L)).willReturn(Optional.of(anotherMember));

            // When
            roomService.enterRoom(1L, request, 2L);

            // Then
            verify(memberRoomRepository).save(any(MemberRoom.class));
            verify(roomSessionService).addMember(1L, 2L);
        }

        @Test
        @DisplayName("성공 - 비밀방 입장")
        void enterRoom_Success_PrivateRoom() {
            // Given
            Room privateRoom = Room.builder()
                    .owner(testMember)
                    .theme(testTheme)
                    .title("비밀 방")
                    .maxPeople(4)
                    .password("1234")
                    .build();
            ReflectionTestUtils.setField(privateRoom, "roomId", 2L);

            RoomEnterRequest request = new RoomEnterRequest();
            ReflectionTestUtils.setField(request, "password", "1234");

            given(roomRepository.findById(2L)).willReturn(Optional.of(privateRoom));
            given(memberRoomRepository.existsByRoom_RoomIdAndMember_MemberId(2L, 2L)).willReturn(false);
            given(memberRoomRepository.countByRoom_RoomId(2L)).willReturn(1L);
            given(memberRepository.findById(2L)).willReturn(Optional.of(anotherMember));

            // When
            roomService.enterRoom(2L, request, 2L);

            // Then
            verify(memberRoomRepository).save(any(MemberRoom.class));
        }

        @Test
        @DisplayName("실패 - 잘못된 비밀번호")
        void enterRoom_Fail_InvalidPassword() {
            // Given
            Room privateRoom = Room.builder()
                    .owner(testMember)
                    .theme(testTheme)
                    .title("비밀 방")
                    .maxPeople(4)
                    .password("1234")
                    .build();
            ReflectionTestUtils.setField(privateRoom, "roomId", 2L);

            RoomEnterRequest request = new RoomEnterRequest();
            ReflectionTestUtils.setField(request, "password", "wrong");

            given(roomRepository.findById(2L)).willReturn(Optional.of(privateRoom));

            // When & Then
            assertThatThrownBy(() -> roomService.enterRoom(2L, request, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_ROOM_PASSWORD);
        }

        @Test
        @DisplayName("실패 - 이미 입장한 방")
        void enterRoom_Fail_AlreadyJoined() {
            // Given
            RoomEnterRequest request = new RoomEnterRequest();

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(memberRoomRepository.existsByRoom_RoomIdAndMember_MemberId(1L, 2L)).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> roomService.enterRoom(1L, request, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_ALREADY_JOINED);
        }

        @Test
        @DisplayName("실패 - 정원 초과")
        void enterRoom_Fail_RoomFull() {
            // Given
            Room fullRoom = Room.builder()
                    .owner(testMember)
                    .theme(testTheme)
                    .title("꽉 찬 방")
                    .maxPeople(2)
                    .build();
            ReflectionTestUtils.setField(fullRoom, "roomId", 3L);

            RoomEnterRequest request = new RoomEnterRequest();

            given(roomRepository.findById(3L)).willReturn(Optional.of(fullRoom));
            given(memberRoomRepository.existsByRoom_RoomIdAndMember_MemberId(3L, 2L)).willReturn(false);
            given(memberRoomRepository.countByRoom_RoomId(3L)).willReturn(2L);

            // When & Then
            assertThatThrownBy(() -> roomService.enterRoom(3L, request, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_FULL);
        }

        @Test
        @DisplayName("실패 - 진행 중인 방 입장 불가")
        void enterRoom_Fail_RoomNotJoinable() {
            // Given
            Room inProgressRoom = Room.builder()
                    .owner(testMember)
                    .theme(testTheme)
                    .title("진행 중인 방")
                    .maxPeople(4)
                    .build();
            ReflectionTestUtils.setField(inProgressRoom, "roomId", 4L);
            inProgressRoom.updateStatus(RoomStatus.IN_PROGRESS);

            RoomEnterRequest request = new RoomEnterRequest();

            given(roomRepository.findById(4L)).willReturn(Optional.of(inProgressRoom));

            // When & Then
            assertThatThrownBy(() -> roomService.enterRoom(4L, request, 2L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_JOINABLE);
        }
    }

    @Nested
    @DisplayName("방 퇴장")
    class LeaveRoom {

        @Test
        @DisplayName("성공 - 일반 멤버 퇴장")
        void leaveRoom_Success_NormalMember() {
            // Given
            Member anotherMember = Member.builder()
                    .email("another@test.com")
                    .password("password")
                    .nickname("다른사용자")
                    .build();
            ReflectionTestUtils.setField(anotherMember, "memberId", 2L);

            MemberRoom memberRoom = MemberRoom.builder()
                    .room(testRoom)
                    .member(anotherMember)
                    .build();

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(memberRoomRepository.findByRoom_RoomIdAndMember_MemberId(1L, 2L))
                    .willReturn(Optional.of(memberRoom));
            given(memberRoomRepository.countByRoom_RoomId(1L)).willReturn(1L);

            // When
            roomService.leaveRoom(1L, 2L);

            // Then
            verify(memberRoomRepository).delete(memberRoom);
            verify(roomSessionService).removeMember(1L, 2L);
        }

        @Test
        @DisplayName("성공 - 방장 퇴장 시 위임")
        void leaveRoom_Success_OwnerLeave_Delegate() {
            // Given
            Member nextOwner = Member.builder()
                    .email("next@test.com")
                    .password("password")
                    .nickname("다음방장")
                    .build();
            ReflectionTestUtils.setField(nextOwner, "memberId", 2L);

            MemberRoom ownerMemberRoom = MemberRoom.builder()
                    .room(testRoom)
                    .member(testMember)
                    .build();

            MemberRoom nextMemberRoom = MemberRoom.builder()
                    .room(testRoom)
                    .member(nextOwner)
                    .build();

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(memberRoomRepository.findByRoom_RoomIdAndMember_MemberId(1L, 1L))
                    .willReturn(Optional.of(ownerMemberRoom));
            given(memberRoomRepository.findFirstByRoomIdOrderByCreatedAtAsc(1L))
                    .willReturn(Optional.of(nextMemberRoom));
            given(memberRoomRepository.countByRoom_RoomId(1L)).willReturn(1L);

            // When
            roomService.leaveRoom(1L, 1L);

            // Then
            assertThat(testRoom.getOwner().getMemberId()).isEqualTo(2L);
        }

        @Test
        @DisplayName("성공 - 마지막 멤버 퇴장 시 방 종료")
        void leaveRoom_Success_LastMemberLeave_RoomCompleted() {
            // Given
            MemberRoom memberRoom = MemberRoom.builder()
                    .room(testRoom)
                    .member(testMember)
                    .build();

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(memberRoomRepository.findByRoom_RoomIdAndMember_MemberId(1L, 1L))
                    .willReturn(Optional.of(memberRoom));
            given(memberRoomRepository.findFirstByRoomIdOrderByCreatedAtAsc(1L))
                    .willReturn(Optional.empty());
            given(memberRoomRepository.countByRoom_RoomId(1L)).willReturn(0L);

            // When
            roomService.leaveRoom(1L, 1L);

            // Then
            assertThat(testRoom.getStatus()).isEqualTo(RoomStatus.COMPLETED);
            verify(roomSessionService).clearRoomSession(1L);
        }

        @Test
        @DisplayName("실패 - 참여하지 않은 방")
        void leaveRoom_Fail_NotMember() {
            // Given
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(memberRoomRepository.findByRoom_RoomIdAndMember_MemberId(1L, 999L))
                    .willReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> roomService.leaveRoom(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_MEMBER_ROOM);
        }
    }

    @Nested
    @DisplayName("역할 확정")
    class ConfirmRoles {

        @Test
        @DisplayName("성공 - 모든 참여자가 역할 선택 완료")
        void confirmRoles_Success() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            Member member2 = Member.builder().email("m2@test.com").password("pw").nickname("M2").build();
            Member member3 = Member.builder().email("m3@test.com").password("pw").nickname("M3").build();
            Member member4 = Member.builder().email("m4@test.com").password("pw").nickname("M4").build();
            ReflectionTestUtils.setField(member2, "memberId", 2L);
            ReflectionTestUtils.setField(member3, "memberId", 3L);
            ReflectionTestUtils.setField(member4, "memberId", 4L);

            MemberRoom mr1 = MemberRoom.builder().room(testRoom).member(testMember).build();
            MemberRoom mr2 = MemberRoom.builder().room(testRoom).member(member2).build();
            MemberRoom mr3 = MemberRoom.builder().room(testRoom).member(member3).build();
            MemberRoom mr4 = MemberRoom.builder().room(testRoom).member(member4).build();

            // 역할 확정 요청 생성
            com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest request =
                new com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest();
            java.util.List<com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment> assignments =
                java.util.Arrays.asList(
                    createRoleAssignment(1L, 1L),
                    createRoleAssignment(2L, 2L),
                    createRoleAssignment(3L, 3L),
                    createRoleAssignment(4L, 4L)
                );
            ReflectionTestUtils.setField(request, "roles", assignments);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROLE_PICK);
            given(roomSessionService.isRolesConfirmed(1L)).willReturn(false);
            given(memberRoomRepository.findByRoomIdWithMember(1L)).willReturn(java.util.List.of(mr1, mr2, mr3, mr4));
            given(roleRepository.existsById(anyLong())).willReturn(true);

            // When
            roomService.confirmRoles(1L, request, 1L);

            // Then
            verify(roomSessionService).clearRoles(1L);
            verify(roomSessionService, times(4)).assignRole(eq(1L), anyLong(), anyLong());
            verify(roomSessionService).setRolesConfirmed(1L, true);
        }

        private com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment createRoleAssignment(Long memberId, Long roleId) {
            com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment assignment =
                new com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment();
            ReflectionTestUtils.setField(assignment, "memberId", memberId);
            ReflectionTestUtils.setField(assignment, "roleId", roleId);
            return assignment;
        }

        @Test
        @DisplayName("실패 - 방장이 아님")
        void confirmRoles_Fail_NotOwner() {
            // Given
            com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest request =
                new com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest();
            java.util.List<com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment> assignments =
                java.util.Arrays.asList(createRoleAssignment(1L, 1L));
            ReflectionTestUtils.setField(request, "roles", assignments);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

            // When & Then
            assertThatThrownBy(() -> roomService.confirmRoles(1L, request, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_OWNER);
        }

        @Test
        @DisplayName("실패 - 진행 중이 아님")
        void confirmRoles_Fail_NotInProgress() {
            // Given
            com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest request =
                new com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest();
            java.util.List<com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment> assignments =
                java.util.Arrays.asList(createRoleAssignment(1L, 1L));
            ReflectionTestUtils.setField(request, "roles", assignments);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

            // When & Then
            assertThatThrownBy(() -> roomService.confirmRoles(1L, request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_IN_PROGRESS);
        }

        @Test
        @DisplayName("실패 - ROLE_PICK 단계가 아님")
        void confirmRoles_Fail_InvalidPhase() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest request =
                new com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest();
            java.util.List<com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment> assignments =
                java.util.Arrays.asList(createRoleAssignment(1L, 1L));
            ReflectionTestUtils.setField(request, "roles", assignments);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.WATCHING);

            // When & Then
            assertThatThrownBy(() -> roomService.confirmRoles(1L, request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PHASE);
        }

        @Test
        @DisplayName("실패 - 역할 개수가 참여자 수와 불일치")
        void confirmRoles_Fail_RoleCountMismatch() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            Member member2 = Member.builder().email("m2@test.com").password("pw").nickname("M2").build();
            Member member3 = Member.builder().email("m3@test.com").password("pw").nickname("M3").build();
            Member member4 = Member.builder().email("m4@test.com").password("pw").nickname("M4").build();
            ReflectionTestUtils.setField(member2, "memberId", 2L);
            ReflectionTestUtils.setField(member3, "memberId", 3L);
            ReflectionTestUtils.setField(member4, "memberId", 4L);

            MemberRoom mr1 = MemberRoom.builder().room(testRoom).member(testMember).build();
            MemberRoom mr2 = MemberRoom.builder().room(testRoom).member(member2).build();
            MemberRoom mr3 = MemberRoom.builder().room(testRoom).member(member3).build();
            MemberRoom mr4 = MemberRoom.builder().room(testRoom).member(member4).build();

            // 역할 확정 요청 생성 (2개만 - 불일치)
            com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest request =
                new com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest();
            java.util.List<com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment> assignments =
                java.util.Arrays.asList(
                    createRoleAssignment(1L, 1L),
                    createRoleAssignment(2L, 2L)
                );
            ReflectionTestUtils.setField(request, "roles", assignments);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROLE_PICK);
            given(roomSessionService.isRolesConfirmed(1L)).willReturn(false);
            given(memberRoomRepository.findByRoomIdWithMember(1L)).willReturn(java.util.List.of(mr1, mr2, mr3, mr4));

            // When & Then
            assertThatThrownBy(() -> roomService.confirmRoles(1L, request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLE_COUNT_MISMATCH);
        }

        @Test
        @DisplayName("실패 - 이미 역할이 확정됨")
        void confirmRoles_Fail_AlreadyConfirmed() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest request =
                new com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest();
            java.util.List<com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment> assignments =
                java.util.Arrays.asList(createRoleAssignment(1L, 1L));
            ReflectionTestUtils.setField(request, "roles", assignments);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROLE_PICK);
            given(roomSessionService.isRolesConfirmed(1L)).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> roomService.confirmRoles(1L, request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLES_ALREADY_CONFIRMED);
        }

        @Test
        @DisplayName("실패 - 참여하지 않은 멤버 포함")
        void confirmRoles_Fail_NotRoomMember() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            MemberRoom mr1 = MemberRoom.builder().room(testRoom).member(testMember).build();

            // 참여하지 않은 멤버 ID (999L) 포함
            com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest request =
                new com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest();
            java.util.List<com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment> assignments =
                java.util.Arrays.asList(createRoleAssignment(999L, 1L));
            ReflectionTestUtils.setField(request, "roles", assignments);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROLE_PICK);
            given(roomSessionService.isRolesConfirmed(1L)).willReturn(false);
            given(memberRoomRepository.findByRoomIdWithMember(1L)).willReturn(java.util.List.of(mr1));

            // When & Then
            assertThatThrownBy(() -> roomService.confirmRoles(1L, request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_MEMBER);
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 역할 ID")
        void confirmRoles_Fail_RoleNotFound() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            MemberRoom mr1 = MemberRoom.builder().room(testRoom).member(testMember).build();

            com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest request =
                new com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest();
            java.util.List<com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment> assignments =
                java.util.Arrays.asList(createRoleAssignment(1L, 999L));
            ReflectionTestUtils.setField(request, "roles", assignments);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROLE_PICK);
            given(roomSessionService.isRolesConfirmed(1L)).willReturn(false);
            given(memberRoomRepository.findByRoomIdWithMember(1L)).willReturn(java.util.List.of(mr1));
            given(roleRepository.existsById(999L)).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> roomService.confirmRoles(1L, request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_FOUND_ROLE);
        }

        @Test
        @DisplayName("실패 - 멤버 중복 할당")
        void confirmRoles_Fail_DuplicateMemberRole() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            Member member2 = Member.builder().email("m2@test.com").password("pw").nickname("M2").build();
            ReflectionTestUtils.setField(member2, "memberId", 2L);

            MemberRoom mr1 = MemberRoom.builder().room(testRoom).member(testMember).build();
            MemberRoom mr2 = MemberRoom.builder().room(testRoom).member(member2).build();

            // 같은 멤버에게 두 개의 역할 할당
            com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest request =
                new com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest();
            java.util.List<com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment> assignments =
                java.util.Arrays.asList(
                    createRoleAssignment(1L, 1L),
                    createRoleAssignment(1L, 2L)  // 중복 멤버
                );
            ReflectionTestUtils.setField(request, "roles", assignments);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROLE_PICK);
            given(roomSessionService.isRolesConfirmed(1L)).willReturn(false);
            given(memberRoomRepository.findByRoomIdWithMember(1L)).willReturn(java.util.List.of(mr1, mr2));
            given(roleRepository.existsById(anyLong())).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> roomService.confirmRoles(1L, request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_MEMBER_ROLE);
        }

        @Test
        @DisplayName("실패 - 역할 중복 할당")
        void confirmRoles_Fail_DuplicateRoleAssignment() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            Member member2 = Member.builder().email("m2@test.com").password("pw").nickname("M2").build();
            ReflectionTestUtils.setField(member2, "memberId", 2L);

            MemberRoom mr1 = MemberRoom.builder().room(testRoom).member(testMember).build();
            MemberRoom mr2 = MemberRoom.builder().room(testRoom).member(member2).build();

            // 같은 역할을 두 멤버에게 할당
            com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest request =
                new com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest();
            java.util.List<com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest.RoleAssignment> assignments =
                java.util.Arrays.asList(
                    createRoleAssignment(1L, 1L),
                    createRoleAssignment(2L, 1L)  // 중복 역할
                );
            ReflectionTestUtils.setField(request, "roles", assignments);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROLE_PICK);
            given(roomSessionService.isRolesConfirmed(1L)).willReturn(false);
            given(memberRoomRepository.findByRoomIdWithMember(1L)).willReturn(java.util.List.of(mr1, mr2));
            given(roleRepository.existsById(anyLong())).willReturn(true);

            // When & Then
            assertThatThrownBy(() -> roomService.confirmRoles(1L, request, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.DUPLICATE_ROLE_ASSIGNMENT);
        }
    }

    @Nested
    @DisplayName("영상 시청 완료")
    class FinishWatching {

        @Test
        @DisplayName("성공 - WATCHING → ROLE_PICK phase 전환")
        void finishWatching_Success() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.WATCHING);

            // When
            roomService.finishWatching(1L, 1L);

            // Then
            verify(roomSessionService).setPhase(1L, GamePhase.ROLE_PICK);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/1/state"), any(com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage.class));
        }

        @Test
        @DisplayName("실패 - 방장이 아님")
        void finishWatching_Fail_NotOwner() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

            // When & Then
            assertThatThrownBy(() -> roomService.finishWatching(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_OWNER);
        }

        @Test
        @DisplayName("실패 - 진행 중인 방이 아님")
        void finishWatching_Fail_NotInProgress() {
            // Given
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

            // When & Then
            assertThatThrownBy(() -> roomService.finishWatching(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_IN_PROGRESS);
        }

        @Test
        @DisplayName("실패 - WATCHING 단계가 아님")
        void finishWatching_Fail_InvalidPhase() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROLE_PICK);

            // When & Then
            assertThatThrownBy(() -> roomService.finishWatching(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PHASE);
        }
    }

    @Nested
    @DisplayName("게임 종료")
    class FinishGame {

        @Test
        @DisplayName("성공 - ROUND_2에서 WAITING으로 복귀")
        void finishGame_Success() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROUND_2);

            // When
            roomService.finishGame(1L, 1L);

            // Then
            assertThat(testRoom.getStatus()).isEqualTo(RoomStatus.WAITING);
            verify(roomSessionService).resetGameState(1L);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/1/state"), any(com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage.class));
        }

        @Test
        @DisplayName("실패 - 방장이 아님")
        void finishGame_Fail_NotOwner() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

            // When & Then
            assertThatThrownBy(() -> roomService.finishGame(1L, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_OWNER);
        }

        @Test
        @DisplayName("실패 - 진행 중인 방이 아님")
        void finishGame_Fail_NotInProgress() {
            // Given
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

            // When & Then
            assertThatThrownBy(() -> roomService.finishGame(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROOM_NOT_IN_PROGRESS);
        }

        @Test
        @DisplayName("실패 - ROUND_2 단계가 아님")
        void finishGame_Fail_InvalidPhase() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROUND_1);

            // When & Then
            assertThatThrownBy(() -> roomService.finishGame(1L, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PHASE);
        }
    }

    @Nested
    @DisplayName("라운드 시작")
    class StartRound {

        private com.ssafy.meari.domain.content.entity.Content testContent;
        private com.ssafy.meari.domain.content.entity.Role role1;
        private com.ssafy.meari.domain.content.entity.Role role2;
        private Member member2;
        private MemberRoom mr1;
        private MemberRoom mr2;

        @BeforeEach
        void setUp() {
            testContent = com.ssafy.meari.domain.content.entity.Content.builder()
                    .title("Test Video")
                    .videoUrl("http://test.com/video.mp4")
                    .build();
            ReflectionTestUtils.setField(testContent, "contentId", 1L);

            member2 = Member.builder().email("m2@test.com").password("pw").nickname("M2").build();
            ReflectionTestUtils.setField(member2, "memberId", 2L);

            mr1 = MemberRoom.builder().room(testRoom).member(testMember).build();
            mr2 = MemberRoom.builder().room(testRoom).member(member2).build();

            role1 = com.ssafy.meari.domain.content.entity.Role.builder().content(testContent).name("화자A").build();
            role2 = com.ssafy.meari.domain.content.entity.Role.builder().content(testContent).name("화자B").build();
            ReflectionTestUtils.setField(role1, "roleId", 1L);
            ReflectionTestUtils.setField(role2, "roleId", 2L);
        }

        @Test
        @DisplayName("성공 - Round1 시작 및 역할 DB 저장")
        void startRound_Success_Round1() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            com.ssafy.meari.domain.content.entity.Sentence sentence1 = com.ssafy.meari.domain.content.entity.Sentence.builder()
                    .role(role1).content(testContent).sequence(1)
                    .startTime(java.math.BigDecimal.valueOf(0.0)).endTime(java.math.BigDecimal.valueOf(3.5))
                    .textKo("안녕하세요").textVn("Xin chào").build();
            ReflectionTestUtils.setField(sentence1, "sentenceId", 10L);

            com.ssafy.meari.domain.content.entity.Sentence sentence2 = com.ssafy.meari.domain.content.entity.Sentence.builder()
                    .role(role2).content(testContent).sequence(2)
                    .startTime(java.math.BigDecimal.valueOf(3.5)).endTime(java.math.BigDecimal.valueOf(7.0))
                    .textKo("반갑습니다").textVn("Rất vui").build();
            ReflectionTestUtils.setField(sentence2, "sentenceId", 11L);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROLE_PICK);
            given(roomSessionService.isRolesConfirmed(1L)).willReturn(true);
            given(roomSessionService.getContentId(1L)).willReturn(1L);
            given(contentRepository.findById(1L)).willReturn(Optional.of(testContent));
            given(memberRoomRepository.findByRoomIdWithMember(1L)).willReturn(java.util.List.of(mr1, mr2));
            given(roomSessionService.getAllRoles(1L)).willReturn(java.util.Map.of(1L, "1", 2L, "2"));
            given(roleRepository.findById(1L)).willReturn(Optional.of(role1));
            given(roleRepository.findById(2L)).willReturn(Optional.of(role2));
            given(sentenceRepository.findByContent_ContentId(1L)).willReturn(java.util.List.of(sentence1, sentence2));

            // When
            roomService.startRound(1L, 1, 1L);

            // Then
            verify(shadowingReportRepository, times(2)).save(any());
            verify(roomSessionService).setPhase(1L, GamePhase.ROUND_1);
            verify(roomSessionService).setMemberTotalSentences(1L, 1, 1L, 1);
            verify(roomSessionService).setMemberTotalSentences(1L, 1, 2L, 1);
            verify(roomSessionService).setRoundStartTime(eq(1L), anyLong());
            verify(messagingTemplate).convertAndSend(eq("/topic/room/1/state"), any(com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage.class));
        }

        @Test
        @DisplayName("성공 - Round1 시작 시 ROUND_START 메시지에 segments 포함")
        void startRound_Success_Round1_BroadcastWithSegments() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            com.ssafy.meari.domain.content.entity.Sentence sentence1 = com.ssafy.meari.domain.content.entity.Sentence.builder()
                    .role(role1).content(testContent).sequence(1)
                    .startTime(java.math.BigDecimal.valueOf(0.0)).endTime(java.math.BigDecimal.valueOf(3.5))
                    .textKo("안녕하세요").textVn("Xin chào").build();
            ReflectionTestUtils.setField(sentence1, "sentenceId", 10L);

            com.ssafy.meari.domain.content.entity.Sentence sentence2 = com.ssafy.meari.domain.content.entity.Sentence.builder()
                    .role(role2).content(testContent).sequence(2)
                    .startTime(java.math.BigDecimal.valueOf(3.5)).endTime(java.math.BigDecimal.valueOf(7.0))
                    .textKo("반갑습니다").textVn("Rất vui").build();
            ReflectionTestUtils.setField(sentence2, "sentenceId", 11L);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROLE_PICK);
            given(roomSessionService.isRolesConfirmed(1L)).willReturn(true);
            given(roomSessionService.getContentId(1L)).willReturn(1L);
            given(contentRepository.findById(1L)).willReturn(Optional.of(testContent));
            given(memberRoomRepository.findByRoomIdWithMember(1L)).willReturn(java.util.List.of(mr1, mr2));
            given(roomSessionService.getAllRoles(1L)).willReturn(java.util.Map.of(1L, "1", 2L, "2"));
            given(roleRepository.findById(1L)).willReturn(Optional.of(role1));
            given(roleRepository.findById(2L)).willReturn(Optional.of(role2));
            given(sentenceRepository.findByContent_ContentId(1L)).willReturn(java.util.List.of(sentence1, sentence2));

            // When
            roomService.startRound(1L, 1, 1L);

            // Then
            org.mockito.ArgumentCaptor<com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage> captor =
                    org.mockito.ArgumentCaptor.forClass(com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/room/1/state"), captor.capture());

            com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage sentMessage = captor.getValue();
            assertThat(sentMessage.getType()).isEqualTo("ROUND_START");
            assertThat(sentMessage.getPhase()).isEqualTo(GamePhase.ROUND_1);
            assertThat(sentMessage.getRound()).isEqualTo(1);
            assertThat(sentMessage.getServerTime()).isNotNull();
            assertThat(sentMessage.getSegments()).hasSize(2);
            assertThat(sentMessage.getSegments().get(0).getSentences()).hasSize(1);
            assertThat(sentMessage.getSegments().get(1).getSentences()).hasSize(1);
        }

        @Test
        @DisplayName("성공 - Round2 시작")
        void startRound_Success_Round2() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            com.ssafy.meari.domain.content.entity.Sentence sentence1 = com.ssafy.meari.domain.content.entity.Sentence.builder()
                    .role(role1).content(testContent).sequence(1)
                    .startTime(java.math.BigDecimal.valueOf(0.0)).endTime(java.math.BigDecimal.valueOf(3.5))
                    .textKo("안녕하세요").textVn("Xin chào").build();
            ReflectionTestUtils.setField(sentence1, "sentenceId", 10L);

            com.ssafy.meari.domain.content.entity.Sentence sentence2 = com.ssafy.meari.domain.content.entity.Sentence.builder()
                    .role(role2).content(testContent).sequence(2)
                    .startTime(java.math.BigDecimal.valueOf(3.5)).endTime(java.math.BigDecimal.valueOf(7.0))
                    .textKo("반갑습니다").textVn("Rất vui").build();
            ReflectionTestUtils.setField(sentence2, "sentenceId", 11L);

            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROUND_1);
            given(roomSessionService.isRolesConfirmed(1L)).willReturn(true);
            given(roomSessionService.getContentId(1L)).willReturn(1L);
            given(contentRepository.findById(1L)).willReturn(Optional.of(testContent));
            given(memberRoomRepository.findByRoomIdWithMember(1L)).willReturn(java.util.List.of(mr1, mr2));
            given(roomSessionService.getAllRoles(1L)).willReturn(java.util.Map.of(1L, "1", 2L, "2"));
            given(roleRepository.findById(1L)).willReturn(Optional.of(role1));
            given(roleRepository.findById(2L)).willReturn(Optional.of(role2));
            given(sentenceRepository.findByContent_ContentId(1L)).willReturn(java.util.List.of(sentence1, sentence2));

            // When
            roomService.startRound(1L, 2, 1L);

            // Then
            verify(shadowingReportRepository, never()).save(any());
            verify(roomSessionService).setPhase(1L, GamePhase.ROUND_2);
            verify(messagingTemplate).convertAndSend(eq("/topic/room/1/state"), any(com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage.class));
        }

        @Test
        @DisplayName("실패 - 방장이 아님")
        void startRound_Fail_NotOwner() {
            // Given
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));

            // When & Then
            assertThatThrownBy(() -> roomService.startRound(1L, 1, 999L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_OWNER);
        }

        @Test
        @DisplayName("실패 - ROLE_PICK이 아닌 phase에서 Round1 시작 시도")
        void startRound_Fail_InvalidPhase_Round1() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.WATCHING);

            // When & Then
            assertThatThrownBy(() -> roomService.startRound(1L, 1, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PHASE);
        }

        @Test
        @DisplayName("실패 - 역할이 확정되지 않음")
        void startRound_Fail_RolesNotConfirmed() {
            // Given
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);
            given(roomRepository.findById(1L)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(1L)).willReturn(GamePhase.ROLE_PICK);
            given(roomSessionService.isRolesConfirmed(1L)).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> roomService.startRound(1L, 1, 1L))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.ROLES_NOT_CONFIRMED);
        }
    }

    @Nested
    @DisplayName("녹음 완료 처리")
    class RecordingComplete {

        @Test
        @DisplayName("성공 - 단일 문장 녹음 완료 마킹 (전체 완료 아님)")
        void recordingComplete_Success_SingleSentence_NotComplete() {
            // Given
            Long roomId = 1L;
            com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage message =
                    new com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage();
            message.setMemberId(1L);
            message.setSentenceId(10L);
            message.setAudioUrl("https://s3.example.com/audio/1_10.webm");

            given(roomSessionService.getPhase(roomId)).willReturn(GamePhase.ROUND_1);
            given(roomSessionService.isMember(roomId, 1L)).willReturn(true);
            given(roomSessionService.isAllRecordingsComplete(roomId, 1)).willReturn(false);

            // When
            roomService.recordingComplete(roomId, message);

            // Then
            verify(roomSessionService).markRecordingComplete(roomId, 1, 1L, 10L);
            verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
        }

        @Test
        @DisplayName("성공 - 마지막 문장 완료 시 RECORDINGS_COMPLETE 브로드캐스트")
        void recordingComplete_Success_AllComplete_Broadcast() {
            // Given
            Long roomId = 1L;
            com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage message =
                    new com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage();
            message.setMemberId(2L);
            message.setSentenceId(11L);
            message.setAudioUrl("https://s3.example.com/audio/2_11.webm");

            given(roomSessionService.getPhase(roomId)).willReturn(GamePhase.ROUND_1);
            given(roomSessionService.isMember(roomId, 2L)).willReturn(true);
            given(roomSessionService.isAllRecordingsComplete(roomId, 1)).willReturn(true);

            // When
            roomService.recordingComplete(roomId, message);

            // Then
            verify(roomSessionService).markRecordingComplete(roomId, 1, 2L, 11L);

            org.mockito.ArgumentCaptor<com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage> captor =
                    org.mockito.ArgumentCaptor.forClass(com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage.class);
            verify(messagingTemplate).convertAndSend(eq("/topic/room/" + roomId + "/state"), captor.capture());

            com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage sentMessage = captor.getValue();
            assertThat(sentMessage.getType()).isEqualTo("RECORDINGS_COMPLETE");
            assertThat(sentMessage.getPhase()).isEqualTo(GamePhase.ROUND_1);
            assertThat(sentMessage.getRound()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - Round2에서 녹음 완료 처리")
        void recordingComplete_Success_Round2() {
            // Given
            Long roomId = 1L;
            com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage message =
                    new com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage();
            message.setMemberId(1L);
            message.setSentenceId(10L);

            given(roomSessionService.getPhase(roomId)).willReturn(GamePhase.ROUND_2);
            given(roomSessionService.isMember(roomId, 1L)).willReturn(true);
            given(roomSessionService.isAllRecordingsComplete(roomId, 2)).willReturn(false);

            // When
            roomService.recordingComplete(roomId, message);

            // Then
            verify(roomSessionService).markRecordingComplete(roomId, 2, 1L, 10L);
        }

        @Test
        @DisplayName("실패 - 현재 phase가 ROUND가 아닌 경우")
        void recordingComplete_Fail_InvalidPhase() {
            // Given
            Long roomId = 1L;
            com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage message =
                    new com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage();
            message.setMemberId(1L);
            message.setSentenceId(10L);

            given(roomSessionService.getPhase(roomId)).willReturn(GamePhase.WATCHING);

            // When & Then
            assertThatThrownBy(() -> roomService.recordingComplete(roomId, message))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_PHASE);
        }

        @Test
        @DisplayName("실패 - 해당 방의 멤버가 아닌 경우")
        void recordingComplete_Fail_NotRoomMember() {
            // Given
            Long roomId = 1L;
            com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage message =
                    new com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage();
            message.setMemberId(999L);
            message.setSentenceId(10L);

            given(roomSessionService.getPhase(roomId)).willReturn(GamePhase.ROUND_1);
            given(roomSessionService.isMember(roomId, 999L)).willReturn(false);

            // When & Then
            assertThatThrownBy(() -> roomService.recordingComplete(roomId, message))
                    .isInstanceOf(BusinessException.class)
                    .hasFieldOrPropertyWithValue("errorCode", ErrorCode.NOT_ROOM_MEMBER);
        }
    }
}
