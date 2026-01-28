package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.room.dto.request.RoomEnterRequest;
import com.ssafy.meari.domain.room.dto.websocket.RoomStateMessage;
import com.ssafy.meari.domain.room.entity.GamePhase;
import com.ssafy.meari.domain.room.entity.MemberRoom;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.entity.RoomStatus;
import com.ssafy.meari.domain.room.repository.MemberRoomRepository;
import com.ssafy.meari.domain.room.repository.RoomRepository;
import com.ssafy.meari.domain.theme.entity.Theme;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService WebSocket 단위 테스트")
class RoomServiceWebSocketTest {

    @InjectMocks
    private RoomService roomService;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private RoomRepository roomRepository;

    @Mock
    private MemberRoomRepository memberRoomRepository;

    @Mock
    private MemberRepository memberRepository;

    @Mock
    private ContentRepository contentRepository;

    @Mock
    private RoomSessionService roomSessionService;

    private Member testMember;
    private Theme testTheme;
    private Room testRoom;
    private Content testContent;

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

        testContent = Content.builder()
                .title("Test Video")
                .videoUrl("http://test.com/video.mp4")
                .build();
        ReflectionTestUtils.setField(testContent, "contentId", 1L);
    }

    @Nested
    @DisplayName("방 입장 WebSocket 브로드캐스트")
    class EnterRoomBroadcast {

        @Test
        @DisplayName("성공 - 방 입장 시 MEMBER_JOIN 메시지 브로드캐스트")
        void enterRoom_Success_BroadcastMemberJoin() {
            // Given
            Long roomId = 1L;
            Long memberId = 2L;
            String nickname = "새참여자";

            Member newMember = Member.builder()
                    .email("new@test.com")
                    .password("password")
                    .nickname(nickname)
                    .build();
            ReflectionTestUtils.setField(newMember, "memberId", memberId);

            RoomEnterRequest request = new RoomEnterRequest();

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(memberRoomRepository.existsByRoom_RoomIdAndMember_MemberId(roomId, memberId)).willReturn(false);
            given(memberRoomRepository.countByRoom_RoomId(roomId)).willReturn(1L);
            given(memberRepository.findById(memberId)).willReturn(Optional.of(newMember));

            // When
            roomService.enterRoom(roomId, request, memberId);

            // Then
            ArgumentCaptor<RoomStateMessage> messageCaptor = ArgumentCaptor.forClass(RoomStateMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/state"),
                    messageCaptor.capture()
            );

            RoomStateMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getType()).isEqualTo("MEMBER_JOIN");
            assertThat(sentMessage.getMemberId()).isEqualTo(memberId);
            assertThat(sentMessage.getNickname()).isEqualTo(nickname);
        }
    }

    @Nested
    @DisplayName("방 퇴장 WebSocket 브로드캐스트")
    class LeaveRoomBroadcast {

        @Test
        @DisplayName("성공 - 일반 멤버 퇴장 시 MEMBER_LEAVE 메시지 브로드캐스트")
        void leaveRoom_Success_BroadcastMemberLeave() {
            // Given
            Long roomId = 1L;
            Long memberId = 2L;

            Member leavingMember = Member.builder()
                    .email("leaving@test.com")
                    .password("password")
                    .nickname("퇴장자")
                    .build();
            ReflectionTestUtils.setField(leavingMember, "memberId", memberId);

            MemberRoom memberRoom = MemberRoom.builder()
                    .room(testRoom)
                    .member(leavingMember)
                    .build();

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(memberRoomRepository.findByRoom_RoomIdAndMember_MemberId(roomId, memberId))
                    .willReturn(Optional.of(memberRoom));
            given(memberRoomRepository.countByRoom_RoomId(roomId)).willReturn(1L);

            // When
            roomService.leaveRoom(roomId, memberId);

            // Then
            ArgumentCaptor<RoomStateMessage> messageCaptor = ArgumentCaptor.forClass(RoomStateMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/state"),
                    messageCaptor.capture()
            );

            RoomStateMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getType()).isEqualTo("MEMBER_LEAVE");
            assertThat(sentMessage.getMemberId()).isEqualTo(memberId);
            assertThat(sentMessage.getNewOwnerId()).isNull();
        }

        @Test
        @DisplayName("성공 - 방장 퇴장 시 새 방장 정보 포함하여 브로드캐스트")
        void leaveRoom_Success_OwnerLeave_BroadcastWithNewOwner() {
            // Given
            Long roomId = 1L;
            Long ownerId = 1L;
            Long newOwnerId = 2L;

            Member newOwner = Member.builder()
                    .email("newowner@test.com")
                    .password("password")
                    .nickname("새방장")
                    .build();
            ReflectionTestUtils.setField(newOwner, "memberId", newOwnerId);

            MemberRoom ownerMemberRoom = MemberRoom.builder()
                    .room(testRoom)
                    .member(testMember)
                    .build();

            MemberRoom nextMemberRoom = MemberRoom.builder()
                    .room(testRoom)
                    .member(newOwner)
                    .build();

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(memberRoomRepository.findByRoom_RoomIdAndMember_MemberId(roomId, ownerId))
                    .willReturn(Optional.of(ownerMemberRoom));
            given(memberRoomRepository.findFirstByRoomIdOrderByCreatedAtAsc(roomId))
                    .willReturn(Optional.of(nextMemberRoom));
            given(memberRoomRepository.countByRoom_RoomId(roomId)).willReturn(1L);

            // When
            roomService.leaveRoom(roomId, ownerId);

            // Then
            ArgumentCaptor<RoomStateMessage> messageCaptor = ArgumentCaptor.forClass(RoomStateMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/state"),
                    messageCaptor.capture()
            );

            RoomStateMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getType()).isEqualTo("MEMBER_LEAVE");
            assertThat(sentMessage.getMemberId()).isEqualTo(ownerId);
            assertThat(sentMessage.getNewOwnerId()).isEqualTo(newOwnerId);
        }
    }

    @Nested
    @DisplayName("동영상 선택 WebSocket 브로드캐스트")
    class SelectContentBroadcast {

        @Test
        @DisplayName("성공 - 동영상 선택 시 CONTENT_SELECTED 메시지 브로드캐스트")
        void selectContent_Success_BroadcastContentSelected() {
            // Given
            Long roomId = 1L;
            Long contentId = 1L;
            Long memberId = 1L;

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(contentRepository.existsById(contentId)).willReturn(true);

            // When
            roomService.selectContent(roomId, contentId, memberId);

            // Then
            ArgumentCaptor<RoomStateMessage> messageCaptor = ArgumentCaptor.forClass(RoomStateMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/state"),
                    messageCaptor.capture()
            );

            RoomStateMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getType()).isEqualTo("CONTENT_SELECTED");
            assertThat(sentMessage.getContentId()).isEqualTo(contentId);
        }
    }

    @Nested
    @DisplayName("영상 시청 완료 WebSocket 브로드캐스트")
    class FinishWatchingBroadcast {

        @Test
        @DisplayName("성공 - PHASE_CHANGE(ROLE_PICK) 메시지 브로드캐스트")
        void finishWatching_Success_BroadcastPhaseChange() {
            // Given
            Long roomId = 1L;
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(roomId)).willReturn(GamePhase.WATCHING);

            // When
            roomService.finishWatching(roomId, 1L);

            // Then
            ArgumentCaptor<RoomStateMessage> messageCaptor = ArgumentCaptor.forClass(RoomStateMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/state"),
                    messageCaptor.capture()
            );

            RoomStateMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getType()).isEqualTo("PHASE_CHANGE");
            assertThat(sentMessage.getPhase()).isEqualTo(GamePhase.ROLE_PICK);
        }
    }

    @Nested
    @DisplayName("게임 종료 WebSocket 브로드캐스트")
    class FinishGameBroadcast {

        @Test
        @DisplayName("성공 - PHASE_CHANGE(null) 메시지 브로드캐스트 (WAITING 복귀)")
        void finishGame_Success_BroadcastPhaseChange() {
            // Given
            Long roomId = 1L;
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(roomId)).willReturn(GamePhase.ROUND_2);

            // When
            roomService.finishGame(roomId, 1L);

            // Then
            ArgumentCaptor<RoomStateMessage> messageCaptor = ArgumentCaptor.forClass(RoomStateMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/state"),
                    messageCaptor.capture()
            );

            RoomStateMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getType()).isEqualTo("PHASE_CHANGE");
            assertThat(sentMessage.getPhase()).isNull();
        }
    }

    @Nested
    @DisplayName("게임 시작 WebSocket 브로드캐스트")
    class StartGameBroadcast {

        @Test
        @DisplayName("성공 - 게임 시작 시 GAME_START 메시지 브로드캐스트")
        void startGame_Success_BroadcastGameStart() {
            // Given
            Long roomId = 1L;
            Long contentId = 1L;
            Long memberId = 1L;

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getContentId(roomId)).willReturn(contentId);
            given(memberRoomRepository.findByRoomIdWithMember(roomId)).willReturn(List.of());

            // When
            roomService.startGame(roomId, contentId, memberId);

            // Then
            ArgumentCaptor<RoomStateMessage> messageCaptor = ArgumentCaptor.forClass(RoomStateMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/state"),
                    messageCaptor.capture()
            );

            RoomStateMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getType()).isEqualTo("GAME_START");
            assertThat(sentMessage.getContentId()).isEqualTo(contentId);
            assertThat(sentMessage.getPhase()).isEqualTo(GamePhase.WATCHING);
        }
    }
}
