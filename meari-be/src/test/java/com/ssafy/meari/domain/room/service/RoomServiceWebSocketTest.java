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
import org.junit.jupiter.api.Disabled;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RoomService WebSocket 단위 테스트")
@Disabled("RoomService 분해(refactor.md B1) 시점에 재작성")
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
    private com.ssafy.meari.domain.content.repository.RoleRepository roleRepository;

    @Mock
    private com.ssafy.meari.domain.content.repository.SentenceRepository sentenceRepository;

    @Mock
    private com.ssafy.meari.domain.report.repository.ShadowingReportRepository shadowingReportRepository;

    @Mock
    private com.ssafy.meari.domain.theme.repository.ThemeRepository themeRepository;

    @Mock
    private RoomSessionService roomSessionService;

    @Mock
    private com.ssafy.meari.domain.analysis.service.AnalysisProducer analysisProducer;

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
    @DisplayName("영상 시청 완료(참여자 개인) WebSocket 브로드캐스트")
    class WatchingCompleteBroadcast {

        @Test
        @DisplayName("성공 - 4명 모두 완료 시 PHASE_CHANGE(ROLE_PICK) 메시지 브로드캐스트")
        void watchingComplete_Success_AllComplete_BroadcastPhaseChange() {
            // Given
            Long roomId = 1L;
            Long memberId = 1L;
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(roomId)).willReturn(GamePhase.WATCHING);
            given(roomSessionService.isMember(roomId, memberId)).willReturn(true);
            given(roomSessionService.isAllWatchingComplete(roomId)).willReturn(true);

            // When
            roomService.watchingComplete(roomId, memberId);

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
        @DisplayName("성공 - GAME_FINISHED 메시지 브로드캐스트 (WAITING 복귀)")
        void finishGame_Success_BroadcastGameFinished() {
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
            assertThat(sentMessage.getType()).isEqualTo("GAME_FINISHED");
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

    @Nested
    @DisplayName("Round 시작 WebSocket 브로드캐스트")
    class StartRoundBroadcast {

        @Test
        @DisplayName("성공 - Round1 시작 시 ROUND_START 메시지 브로드캐스트 (segments 포함)")
        void startRound_Success_BroadcastRoundStart() {
            // Given
            Long roomId = 1L;
            testRoom.updateStatus(RoomStatus.IN_PROGRESS);

            Member member2 = Member.builder().email("m2@test.com").password("pw").nickname("M2").build();
            ReflectionTestUtils.setField(member2, "memberId", 2L);

            MemberRoom mr1 = MemberRoom.builder().room(testRoom).member(testMember).build();
            MemberRoom mr2 = MemberRoom.builder().room(testRoom).member(member2).build();

            com.ssafy.meari.domain.content.entity.Role role1 = com.ssafy.meari.domain.content.entity.Role.builder()
                    .content(testContent).name("화자A").build();
            com.ssafy.meari.domain.content.entity.Role role2 = com.ssafy.meari.domain.content.entity.Role.builder()
                    .content(testContent).name("화자B").build();
            ReflectionTestUtils.setField(role1, "roleId", 1L);
            ReflectionTestUtils.setField(role2, "roleId", 2L);

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

            given(roomRepository.findById(roomId)).willReturn(Optional.of(testRoom));
            given(roomSessionService.getPhase(roomId)).willReturn(GamePhase.ROLE_PICK);
            given(roomSessionService.isRolesConfirmed(roomId)).willReturn(true);
            given(roomSessionService.getContentId(roomId)).willReturn(1L);
            given(contentRepository.findById(1L)).willReturn(Optional.of(testContent));
            given(memberRoomRepository.findByRoomIdWithMember(roomId)).willReturn(List.of(mr1, mr2));
            given(roomSessionService.getAllRoles(roomId)).willReturn(java.util.Map.of(1L, "1", 2L, "2"));
            given(roleRepository.findById(1L)).willReturn(Optional.of(role1));
            given(roleRepository.findById(2L)).willReturn(Optional.of(role2));
            given(sentenceRepository.findByContent_ContentId(1L)).willReturn(List.of(sentence1, sentence2));

            // When
            roomService.startRound(roomId, 1, 1L);

            // Then
            ArgumentCaptor<RoomStateMessage> messageCaptor = ArgumentCaptor.forClass(RoomStateMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/state"),
                    messageCaptor.capture()
            );

            RoomStateMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getType()).isEqualTo("ROUND_START");
            assertThat(sentMessage.getPhase()).isEqualTo(GamePhase.ROUND_1);
            assertThat(sentMessage.getRound()).isEqualTo(1);
            assertThat(sentMessage.getServerTime()).isNotNull();
            assertThat(sentMessage.getSegments()).hasSize(2);

            // 멤버1의 세그먼트 확인
            com.ssafy.meari.domain.room.dto.websocket.MemberSegmentInfo segment1 = sentMessage.getSegments().stream()
                    .filter(s -> s.getMemberId().equals(1L)).findFirst().orElseThrow();
            assertThat(segment1.getRoleId()).isEqualTo(1L);
            assertThat(segment1.getRoleName()).isEqualTo("화자A");
            assertThat(segment1.getSentences()).hasSize(1);
            assertThat(segment1.getSentences().get(0).getSentenceId()).isEqualTo(10L);
            assertThat(segment1.getSentences().get(0).getTextKo()).isEqualTo("안녕하세요");
        }
    }

    @Nested
    @DisplayName("녹음 완료 WebSocket 브로드캐스트")
    class RecordingCompleteBroadcast {

        @Test
        @DisplayName("성공 - 모든 멤버 완료 시 RECORDINGS_COMPLETE 메시지 브로드캐스트")
        void recordingComplete_Success_BroadcastRecordingsComplete() {
            // Given
            Long roomId = 1L;
            com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage message =
                    new com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage();
            message.setMemberId(2L);
            message.setSentenceId(11L);

            given(roomSessionService.getPhase(roomId)).willReturn(GamePhase.ROUND_1);
            given(roomSessionService.isMember(roomId, 2L)).willReturn(true);
            given(roomSessionService.isMemberRecordingsComplete(roomId, 1, 2L)).willReturn(false);
            given(roomSessionService.isRoundCompleted(roomId, 1)).willReturn(false);
            given(roomSessionService.getRoundTimeout(roomId, 1)).willReturn(null);
            given(roomSessionService.isAllWatchingComplete(roomId, 1)).willReturn(true);
            given(roomSessionService.isAllRecordingsComplete(roomId, 1)).willReturn(true);

            // When
            roomService.recordingComplete(roomId, message);

            // Then
            ArgumentCaptor<RoomStateMessage> messageCaptor = ArgumentCaptor.forClass(RoomStateMessage.class);
            verify(messagingTemplate).convertAndSend(
                    eq("/topic/room/" + roomId + "/state"),
                    messageCaptor.capture()
            );

            RoomStateMessage sentMessage = messageCaptor.getValue();
            assertThat(sentMessage.getType()).isEqualTo("RECORDINGS_COMPLETE");
            assertThat(sentMessage.getPhase()).isEqualTo(GamePhase.ROUND_1);
            assertThat(sentMessage.getRound()).isEqualTo(1);
        }

        @Test
        @DisplayName("성공 - 미완료 상태이면 브로드캐스트 없음")
        void recordingComplete_Success_NotComplete_NoBroadcast() {
            // Given
            Long roomId = 1L;
            com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage message =
                    new com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage();
            message.setMemberId(1L);
            message.setSentenceId(10L);

            given(roomSessionService.getPhase(roomId)).willReturn(GamePhase.ROUND_2);
            given(roomSessionService.isMember(roomId, 1L)).willReturn(true);
            given(roomSessionService.isMemberRecordingsComplete(roomId, 2, 1L)).willReturn(false);
            given(roomSessionService.isRoundCompleted(roomId, 2)).willReturn(false);
            given(roomSessionService.getRoundTimeout(roomId, 2)).willReturn(null);
            given(roomSessionService.isAllWatchingComplete(roomId, 2)).willReturn(false);

            // When
            roomService.recordingComplete(roomId, message);

            // Then
            verify(roomSessionService).markRecordingComplete(roomId, 2, 1L, 10L);
            verify(messagingTemplate, never()).convertAndSend(any(String.class), any(Object.class));
        }
    }
}
