package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.room.dto.request.RoomCreateRequest;
import com.ssafy.meari.domain.room.dto.request.RoomEnterRequest;
import com.ssafy.meari.domain.room.dto.response.RoomDetailResponse;
import com.ssafy.meari.domain.room.dto.response.RoomListResponse;
import com.ssafy.meari.domain.room.dto.response.RoomResponse;
import com.ssafy.meari.global.common.CursorPageResponse;
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
}
