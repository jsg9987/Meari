import { useState, useEffect, useRef, useCallback } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Users, MessageCircle, Lock, Unlock, Copy, Check, X, LayoutList, LayoutGrid, Maximize2, UserCircle, Play, Home, Clock } from "lucide-react";
// import Header from "../components/common/Header";
import VideoTile from "../components/webrtc/VideoTile";
import { useAuthStore } from "../store/auth.store";
import VideoControls from "../components/webrtc/VideoControls";
import ChatPanel from "../components/webrtc/ChatPanel";
import MediaCheckScreen from "../components/webrtc/MediaCheckScreen";
import ContentSelectModal from "../components/webrtc/ContentSelectModal";
import Toast from "../components/common/Toast";
import RoleSelectModal from "../components/webrtc/RoleSelectModal";
import { useVideoRoom } from "../hooks/useVideoRoom";
import { useRoomWebSocket, type Role, type RoleSegment, type Sentence, type ChatMessage } from "../hooks/useRoomWebSocket";
import type { Content } from "../api/contents.api";
import { selectRoomContent, getContentRoles, getThemes, getThemeContents, type ContentRole } from "../api/contents.api";
import { getRoomDetail, enterRoom, leaveRoom, startGame, finishWatching, confirmRoles, startRound, finishRound, finishRoom, getContentVideoUrl, getPresignedUrl, uploadRecordingToS3 } from "../api/rooms.api";
import { leaveWebRTC } from "../api/webrtc.api";
import { useRoomStore } from "../store/room.store";
import { useRoleStore } from "../store/role.store";
import { useAudioRecorder } from "../hooks/useAudioRecorder";

type SidebarTab = "video" | "chat";
type LayoutMode = "narrow" | "grid" | "wide";

// ========================================
// WebRTC 비활성화 플래그
// true로 설정하면 WebRTC 없이 쉐도잉 기능만 테스트
// ========================================
const DISABLE_WEBRTC = false;

// TODO: 헤더 변경, 비디오 타일 변경
export default function ShadowingRoom() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const { userInfo } = useAuthStore();
  const roomOwnerId = useRoomStore((state) => state.roomData?.owner_id);
  const isOwner = userInfo?.memberId === roomOwnerId;
  const { roomData, contentId, setRoomData, setContentId, clearRoomData } = useRoomStore();
  const {
    availableRoles,
    mySelectedRoleId,
    selectedRoles,
    setAvailableRoles,
    setMySelectedRole,
    setMemberRole,
    removeMemberRole,
    getConfirmData,
    clearRoles,
  } = useRoleStore();
  const [sidebarTab, setSidebarTab] = useState<SidebarTab>("video");
  const [isRoomLoading, setIsRoomLoading] = useState(true);
  const [isEntered, setIsEntered] = useState(false);
  const [copiedPassword, setCopiedPassword] = useState(false);
  const [layoutMode, setLayoutMode] = useState<LayoutMode>("narrow");
  const [isLayoutDropdownOpen, setIsLayoutDropdownOpen] = useState(false);
  // WebRTC 비활성화 시 미디어 체크 건너뛰기
  const [isMediaChecked, setIsMediaChecked] = useState(DISABLE_WEBRTC ? true : false);
  const [isContentSelectOpen, setIsContentSelectOpen] = useState(false);
  const [selectedContent, setSelectedContent] = useState<Content | null>(null);
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [themeDescription, setThemeDescription] = useState<string>("");
  const [availableContents, setAvailableContents] = useState<Content[]>([]); // 테마별 컨텐츠 목록
  const [isContentsLoading, setIsContentsLoading] = useState(false); // 컨텐츠 목록 로딩 상태
  const [mainContentIndex, setMainContentIndex] = useState(0); // 메인으로 표시할 컨텐츠 인덱스
  const [carouselOrder, setCarouselOrder] = useState<number[]>([]); // 캐러셀 표시 순서
  const [isTransitioning, setIsTransitioning] = useState(false); // 컨텐츠 전환 애니메이션 중
  const [isReady, setIsReady] = useState(false); // 내 준비 상태
  const [isRoleSelectOpen, setIsRoleSelectOpen] = useState(false); // 역할 선택 모달 상태
  const [isConfirmingRoles, setIsConfirmingRoles] = useState(false); // 역할 확정 로딩 상태
  const [isRoleAssigned, setIsRoleAssigned] = useState(false); // 역할 선택 완료 여부
  const [isGameStarting, setIsGameStarting] = useState(false); // 게임 시작 중 여부
  const [isRoundStarting, setIsRoundStarting] = useState(false); // 라운드 시작 중 여부
  const [isFinishingRound, setIsFinishingRound] = useState(false); // 라운드 종료 API 호출 중
  const [currentRound, setCurrentRound] = useState(0); // 현재 라운드 (0: 시작 전)
  const [isRoundInProgress, setIsRoundInProgress] = useState(false); // 라운드 진행 중 여부
  const [isReadyLoading, setIsReadyLoading] = useState(false); // 준비 완료 로딩 상태
  const [isWaitingForRolePick, setIsWaitingForRolePick] = useState(false); // 영상 시청 완료 후 역할 선택 대기 중
  const [participantsReady, setParticipantsReady] = useState<Record<number, boolean>>({});
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const [videoReady, setVideoReady] = useState(false);
  const layoutDropdownRef = useRef<HTMLDivElement>(null);
  const readyTimeoutRef = useRef<number | null>(null);
  const hasEnteredRef = useRef(false); // enterRoom API 중복 호출 방지용
  const previousMembersRef = useRef<Array<{member_id: number; nickname: string}>>([]);
  const memberId = userInfo?.memberId ?? 0;

  // VideoControls 관련 상태
  const [volume, setVolume] = useState(100);
  const [selectedAudioDevice, setSelectedAudioDevice] = useState<string>();
  const [selectedVideoDevice, setSelectedVideoDevice] = useState<string>();
  const [selectedNationality, setSelectedNationality] = useState<"KR" | "VN">("KR");
  const [isSubtitleEnabled, setIsSubtitleEnabled] = useState(true);
  const [isLeaving, setIsLeaving] = useState(false); // 방 나가는 중 상태
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false); // 비밀번호 입력 모달 상태
  const [password, setPassword] = useState(""); // 입력한 비밀번호
  const [passwordError, setPasswordError] = useState(""); // 비밀번호 에러 메시지

  // 영상 재생 관련 상태
  const videoRef = useRef<HTMLVideoElement>(null);
  const needPlayOnCanPlayRef = useRef(false);
  const [isPlaying, setIsPlaying] = useState(false);
  const [countdown, setCountdown] = useState<number | null>(null);
  // Round용 자막 데이터 (여러 역할의 대본을 동시에 표시)
  interface SubtitleItem {
    roleName: string;
    roleId: number;
    text: string;
    isMyRole: boolean;
    timing: 'prev' | 'current' | 'next'; // 이전/현재/다음 구분
    nickname?: string; // 역할을 맡은 유저 닉네임
    nicknameColor?: string; // 닉네임 색깔
  }
  const [currentSubtitles, setCurrentSubtitles] = useState<SubtitleItem[]>([]);
  const [roleSegments, setRoleSegments] = useState<RoleSegment[]>([]);

  // 시간대별로 재가공된 자막 데이터 (빠른 조회용)
  interface TimeIndexedSubtitle {
    sentence_id: number;
    start_time: number;
    end_time: number;
    role_id: number;
    role_name: string;
    text_ko: string;
    text_vn: string;
  }
  const timeIndexedSubtitlesRef = useRef<TimeIndexedSubtitle[]>([]);
  const [timeUntilStart, setTimeUntilStart] = useState<number | null>(null);
  const currentRecordingSentenceIdRef = useRef<number | null>(null);
  const presignedUrlsRef = useRef<Map<number, string>>(new Map());
  const s3KeysRef = useRef<Map<number, string>>(new Map());

  const nickname = userInfo?.nickname || "User";

  // 랜덤 색깔 생성 함수
  const generateUserColor = (memberId: number) => {
    const colors = [
      '#3B82F6', // blue
      '#10B981', // green
      '#F59E0B', // amber
      '#EF4444', // red
      '#8B5CF6', // violet
      '#EC4899', // pink
      '#06B6D4', // cyan
      '#F97316', // orange
    ];
    // memberId 기반 시드로 항상 같은 색깔 반환
    const index = memberId % colors.length;
    return colors[index];
  };

  // 역할 ID로 닉네임 찾기 (selectedRoles에서 member_id 찾고, roomData에서 nickname 찾기)
  const getNicknameByRoleId = useCallback((roleId: number): { nickname: string; memberId: number } | null => {
    // selectedRoles에서 해당 roleId를 선택한 memberId 찾기
    const memberEntry = Object.entries(selectedRoles).find(([, selectedRoleId]) => selectedRoleId === roleId);
    if (!memberEntry) return null;

    const memberId = Number(memberEntry[0]);

    // roomData.members에서 해당 memberId의 nickname 찾기
    const member = roomData?.members.find(m => m.member_id === memberId);
    if (!member) return null;

    return { nickname: member.nickname, memberId };
  }, [selectedRoles, roomData]);

  // 디버깅: memberId와 userInfo 확인
  useEffect(() => {
  }, [userInfo, memberId]);

  // 채팅 메시지 상태
  const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);

  // 녹음 핸들러
  const handleRecordingComplete = async (audioBlob: Blob, sentenceId: number) => {
    try {
      console.log(`Uploading recording for sentence ${sentenceId}, size: ${audioBlob.size} bytes`);

      // 미리 받아놓은 Presigned URL 사용
      const presignedUrl = presignedUrlsRef.current.get(sentenceId);
      const s3Key = s3KeysRef.current.get(sentenceId);

      if (!presignedUrl) {
        console.error(`No presigned URL found for sentence ${sentenceId}`);
        return;
      }

      if (!s3Key) {
        console.error(`No s3_key found for sentence ${sentenceId}`);
        return;
      }

      // S3에 업로드
      await uploadRecordingToS3(presignedUrl, audioBlob);
      console.log(`Successfully uploaded recording for sentence ${sentenceId}`);

      // WebSocket으로 녹음 완료 알림 전송 (s3_key 포함)
      sendRecordingComplete(sentenceId, s3Key);
      console.log(`Recording complete notification sent for sentence ${sentenceId}, s3_key: ${s3Key}`);

      // 사용한 URL과 key 삭제
      presignedUrlsRef.current.delete(sentenceId);
      s3KeysRef.current.delete(sentenceId);
    } catch (error) {
      console.error('Failed to upload recording:', error);
    }
  };

  // 오디오 레코더
  const { startRecording, stopRecording } = useAudioRecorder({
    onRecordingComplete: (audioBlob) => {
      const sentenceId = currentRecordingSentenceIdRef.current;
      if (sentenceId !== null) {
        handleRecordingComplete(audioBlob, sentenceId);
        // 업로드 완료 후 sentenceId 초기화
        currentRecordingSentenceIdRef.current = null;
      } else {
        console.warn('[ShadowingRoom] sentenceId is null, cannot upload');
      }
    },
    onError: (error) => {
      console.error('Recording error:', error);
      setToastMessage('녹음에 실패했습니다');
    },
  });

  // 녹음 함수 안정적 참조 (useEffect deps 재실행 방지)
  const startRecordingRef = useRef(startRecording);
  const stopRecordingRef = useRef(stopRecording);
  useEffect(() => {
    startRecordingRef.current = startRecording;
    stopRecordingRef.current = stopRecording;
  });

  // 웹소켓 연결
  const {
    toggleReady: wsToggleReady,
    assignRole,
    sendChatMessage,
    sendRecordingComplete,
  } = useRoomWebSocket({
    roomId: Number(roomId),
    memberId,
    onContentSelected: async (message) => {
      // 모든 사용자가 content_id를 받아서 비디오 URL 가져오기
      if (!message.content_id) return;

      const contentId = message.content_id;

      // content_id를 store에 저장
      setContentId(contentId);

      let videoUrlFromApi = '';
      try {
        const videoResponse = await getContentVideoUrl(contentId);
        if (videoResponse.data.success && videoResponse.data.data) {
          videoUrlFromApi = videoResponse.data.data.video_url;
          setVideoUrl(videoUrlFromApi);
        }
      } catch (error) {
        console.error('[CONTENT_SELECTED] Failed to get video URL:', error);
        setToastMessage('비디오를 불러오는데 실패했습니다');
        return;
      }

      // 모든 사용자가 비디오를 볼 수 있도록 selectedContent 설정
      // 참가자들은 WebSocket 메시지로만 컨텐츠 정보를 받음
      setSelectedContent((prev) => {
        // 방장이 이미 setSelectedContent를 호출한 경우 덮어쓰지 않음
        if (prev && prev.content_id === contentId) {
          return prev;
        }

        // 참가자들은 임시 객체 생성
        return {
          content_id: contentId,
          theme_id: roomData?.theme_id || 1,
          title: '선택된 컨텐츠', // 실제 제목은 방장이 선택한 컨텐츠에서만 표시됨
          description: '',
          video_url: videoUrlFromApi,
          thumbnail_url: '',
          total_duration: 0,
        };
      });

      // 컨텐츠 선택 시 모든 참가자의 준비 상태 초기화
      setIsReady(false);
      setParticipantsReady({});
    },
    onReady: (message) => {
      // 준비 상태 메시지 수신
      if (message.member_id !== undefined && message.ready !== undefined) {
        console.log('[onReady] Received ready message:', message);
        setParticipantsReady(prev => {
          const updated = {
            ...prev,
            [message.member_id!]: message.ready!,
          };
          console.log('[onReady] Updated participantsReady:', updated);
          return updated;
        });

        // 내 준비 상태 업데이트
        if (message.member_id === memberId) {
          // 타임아웃 클리어
          if (readyTimeoutRef.current) {
            clearTimeout(readyTimeoutRef.current);
            readyTimeoutRef.current = null;
          }
          setIsReadyLoading(false); // 로딩 종료
          setIsReady(message.ready);
        }
      }
    },
    onRolePick: async (message) => {
      // 대기 상태 해제
      setIsWaitingForRolePick(false);

      // store에서 최신 content_id 직접 가져오기 (클로저 문제 해결)
      const storeContentId = useRoomStore.getState().contentId;
      const currentContentId = storeContentId || message.content_id || selectedContent?.content_id;

      if (!currentContentId) {
        console.error('No content_id available to fetch roles');
        setToastMessage('역할 정보를 불러올 수 없습니다');
        return;
      }

      try {
        const response = await getContentRoles(currentContentId);
        if (response.data.success && response.data.data) {
          // ContentRole을 Role 타입으로 변환
          const roles: Role[] = response.data.data.map((role: ContentRole) => ({
            id: role.content_id,
            role_id: role.role_id,
            name: role.name,
            created_at: new Date().toISOString(),
            updated_at: new Date().toISOString(),
          }));

          setAvailableRoles(roles);
          setIsRoleSelectOpen(true);
        } else {
          console.error('Failed to fetch roles:', response.data.error);
          setToastMessage('역할 정보를 불러오는데 실패했습니다');
        }
      } catch (error) {
        console.error('Failed to fetch roles:', error);
        setToastMessage('역할 정보를 불러오는데 실패했습니다');
      }
    },
    onRoleAssigned: (message) => {
      // 역할 선점 성공 시 선택된 역할 ID 저장 (아직 확정은 아님)
      if (message.role_id && message.member_id) {
        // 모든 멤버의 역할 선택 상태는 항상 업데이트
        setMemberRole(message.member_id, message.role_id);

        // 본인이 선택한 경우에만 mySelectedRole 업데이트 및 toast 표시
        if (message.member_id === memberId) {
          console.log('[onRoleAssigned] This is MY selection!');
          setMySelectedRole(message.role_id);
          setToastMessage('역할이 등록되었습니다');
        } else {
          console.log('[onRoleAssigned] This is NOT my selection. Someone else selected.');
        }
      }
    },
    onRoleReleased: (message) => {
      console.log('Role released:', message);
      // 역할 해제 시 처리
      if (message.member_id) {
        // 모든 멤버의 역할 선택 상태에서 해당 멤버 제거
        removeMemberRole(message.member_id);

        // 본인이 해제한 경우에만 mySelectedRole 초기화 및 toast 표시
        if (message.member_id === memberId) {
          setMySelectedRole(undefined);
          setToastMessage('역할이 해제되었습니다');
        }
      }
    },
    onGameStart: (message) => {
      // segments 데이터를 시간대별로 재가공
      if (message.segments) {
        const allSubtitles: TimeIndexedSubtitle[] = [];

        message.segments.forEach((segment) => {
          segment.sentences.forEach((sentence) => {
            allSubtitles.push({
              sentence_id: sentence.sentence_id,
              start_time: sentence.start_time,
              end_time: sentence.end_time,
              role_id: segment.role_id,
              role_name: segment.role_name,
              text_ko: sentence.text_ko,
              text_vn: sentence.text_vn,
            });
          });
        });

        // 시작 시간순으로 정렬
        allSubtitles.sort((a, b) => a.start_time - b.start_time);
        timeIndexedSubtitlesRef.current = allSubtitles;
      }

      // 게임 시작 시 영상 재생 준비
      if (message.phase === 'WATCHING') {
        needPlayOnCanPlayRef.current = true;
        setIsPlaying(true);

        // Fallback: 비디오 메타데이터 로드가 지연되는 경우 대비하여 1.5초 후 재생 시도
        setTimeout(() => {
          if (needPlayOnCanPlayRef.current && videoRef.current) {
            console.log('Fallback: Playing video after 1.5s delay');
            videoRef.current.play().catch(err => {
              console.error('Fallback video play failed:', err);
            });
          }
        }, 1500);
      }
    },
    onPhaseWaiting: () => {
      // 대기 상태로 돌아가기
      resetToWaitingState();
    },
    onRolesConfirmed: (message) => {
      // 대본 데이터 저장
      if (message.segments) {
        setRoleSegments(message.segments);

        // Round 모드용 시간대별 전처리 (모든 역할의 대본 포함)
        const allSubtitles: TimeIndexedSubtitle[] = [];

        message.segments.forEach((segment) => {
          segment.sentences.forEach((sentence) => {
            allSubtitles.push({
              sentence_id: sentence.sentence_id,
              start_time: sentence.start_time,
              end_time: sentence.end_time,
              role_id: segment.role_id,
              role_name: segment.role_name,
              text_ko: sentence.text_ko,
              text_vn: sentence.text_vn,
            });
          });
        });

        // 시작 시간순으로 정렬
        allSubtitles.sort((a, b) => a.start_time - b.start_time);
        timeIndexedSubtitlesRef.current = allSubtitles;

        // 모든 사용자 화면에서 캐릭터 선택 모달과 버튼 닫기
        setIsRoleSelectOpen(false);
        setIsRoleAssigned(true);
        setToastMessage('역할이 확정되었습니다');
      }
    },
    onRoundStart: (message) => {
      // 대본 데이터 저장
      if (message.segments) {
        setRoleSegments(message.segments);
      } else {
        console.warn('No segments in ROUND_START message');
      }

      // 라운드 정보 업데이트
      if (message.round) {
        setCurrentRound(message.round);
        setIsRoundInProgress(true);
        setIsRoundStarting(false);
      }

      // server_time까지 대기
      if (message.server_time) {
        const currentTime = Date.now();
        const timeLeft = message.server_time - currentTime;

        if (timeLeft > 0) {
          // 남은 시간을 초 단위로 표시 (카운트다운)
          setTimeUntilStart(Math.ceil(timeLeft / 1000));
        } else {
          // 이미 시간이 지났으면 즉시 재생 (timeUntilStart를 0으로 설정하여 useEffect에서 처리)
          setTimeUntilStart(0);
        }
      }
    },
    onGameFinished: (message) => {
      console.log('Game finished:', message);
      // 상태 초기화하여 컨텐츠 선택 화면으로 돌아가기
      setIsPlaying(false);
      setVideoReady(false);
      setRoleSegments([]);
      setCurrentSubtitles([]);
      setParticipantsReady({});
      currentRecordingSentenceIdRef.current = null;
      presignedUrlsRef.current.clear();
      s3KeysRef.current.clear();
      timeIndexedSubtitlesRef.current = [];

      // Store 초기화
      setContentId(null);
      clearRoles();

      resetToWaitingState();
      setToastMessage('게임이 종료되었습니다');
    },
    onMemberJoin: async (message) => {
      // 시스템 메시지 추가 (백엔드에서 nickname을 보내주는 경우)
      if (message.nickname) {
        const systemMessage: ChatMessage = {
          sender_id: 0,
          nickname: 'System',
          message: `${message.nickname}님이 입장하셨습니다`,
          timestamp: new Date().toISOString(),
          isSystem: true,
        };
        setChatMessages(prev => [...prev, systemMessage]);
      }

      // 멤버 입장 시 방 정보 다시 가져오기 (members 업데이트)
      if (roomId) {
        try {
          const response = await getRoomDetail(Number(roomId));
          if (response.data.success && response.data.data) {
            setRoomData(response.data.data);

            // 백엔드에서 nickname을 안 보내주는 경우, 새로운 멤버 찾기
            if (!message.nickname && message.member_id) {
              const newMember = response.data.data.members.find(
                m => m.member_id === message.member_id
              );
              if (newMember) {
                const systemMessage: ChatMessage = {
                  sender_id: 0,
                  nickname: 'System',
                  message: `${newMember.nickname}님이 입장하셨습니다`,
                  timestamp: new Date().toISOString(),
                  isSystem: true,
                };
                setChatMessages(prev => [...prev, systemMessage]);
              }
            }

            // 현재 멤버 목록 저장 및 준비 상태 동기화
            const members = response.data.data.members;
            previousMembersRef.current = members.map(m => ({
              member_id: m.member_id,
              nickname: m.nickname,
            }));

            // 멤버들의 준비 상태를 participantsReady에 반영 (기존 상태 유지하면서 업데이트)
            setParticipantsReady(prev => {
              const updated = { ...prev };
              members.forEach(m => {
                updated[m.member_id] = m.is_ready;
              });
              return updated;
            });
          }
        } catch (error) {
          console.error('Failed to refresh room detail:', error);
        }
      }
    },
    onMemberLeave: async (message) => {
      // 백엔드에서 nickname을 보내주는 경우
      let leavingNickname = message.nickname;

      // 백엔드에서 nickname을 안 보내주는 경우, 이전 멤버 목록에서 찾기
      if (!leavingNickname && message.member_id) {
        const previousMember = previousMembersRef.current.find(
          m => m.member_id === message.member_id
        );
        if (previousMember) {
          leavingNickname = previousMember.nickname;
        }
      }

      // 시스템 메시지 추가
      if (leavingNickname) {
        const systemMessage: ChatMessage = {
          sender_id: 0,
          nickname: 'System',
          message: `${leavingNickname}님이 퇴장하셨습니다`,
          timestamp: new Date().toISOString(),
          isSystem: true,
        };
        setChatMessages(prev => [...prev, systemMessage]);
      }

      // 나간 멤버의 준비 상태 제거
      if (message.member_id) {
        setParticipantsReady(prev => {
          const newReady = { ...prev };
          delete newReady[message.member_id!];
          return newReady;
        });
      }

      // 멤버 퇴장 시 방 정보 다시 가져오기 (members 업데이트)
      if (roomId) {
        try {
          const response = await getRoomDetail(Number(roomId));
          if (response.data.success && response.data.data) {
            setRoomData(response.data.data);

            // 현재 멤버 목록 저장 및 준비 상태 동기화
            const members = response.data.data.members;
            previousMembersRef.current = members.map(m => ({
              member_id: m.member_id,
              nickname: m.nickname,
            }));

            // 멤버들의 준비 상태를 participantsReady에 반영 (서버 상태로 완전 동기화)
            const updatedReadyState: Record<number, boolean> = {};
            members.forEach(m => {
              updatedReadyState[m.member_id] = m.is_ready;
            });
            setParticipantsReady(updatedReadyState);
          }
        } catch (error) {
          console.error('Failed to refresh room detail:', error);
        }
      }
    },
    onError: (error) => {
      console.error('WebSocket error:', error);
    },
    onChatMessage: (message) => {
      setChatMessages(prev => [...prev, message]);
    },
  });

  // 방 정보 가져오기
  useEffect(() => {
    const fetchRoomDetail = async () => {
      if (!roomId) return;

      // React Strict Mode에서 중복 호출 방지
      if (hasEnteredRef.current) {
        return;
      }
      hasEnteredRef.current = true;

      try {
        setIsRoomLoading(true);
        const response = await getRoomDetail(Number(roomId));

        if (!response.data.success || !response.data.data) {
          alert('방 정보를 가져올 수 없습니다.');
          navigate('/');
          return;
        }

        setRoomData(response.data.data);

        // 초기 멤버 목록 저장 및 준비 상태 동기화
        previousMembersRef.current = response.data.data.members.map(m => ({
          member_id: m.member_id,
          nickname: m.nickname,
        }));

        // 초기 멤버들의 준비 상태를 participantsReady에 반영
        const initialReadyState: Record<number, boolean> = {};
        response.data.data.members.forEach(m => {
          initialReadyState[m.member_id] = m.is_ready;
        });
        console.log('[fetchRoomDetail] Initial participantsReady:', initialReadyState);
        setParticipantsReady(initialReadyState);

        // content_id가 이미 있으면 video_url 가져오기
        if (response.data.data.content_id) {
          const existingContentId = response.data.data.content_id;
          setContentId(existingContentId);

          try {
            const videoResponse = await getContentVideoUrl(existingContentId);
            if (videoResponse.data.success && videoResponse.data.data) {
              const videoUrlFromApi = videoResponse.data.data.video_url;
              setVideoUrl(videoUrlFromApi);

              // 임시 컨텐츠 객체 생성
              setSelectedContent({
                content_id: existingContentId,
                theme_id: response.data.data.theme_id || 1,
                title: '선택된 컨텐츠',
                description: '',
                video_url: videoUrlFromApi,
                thumbnail_url: '',
                total_duration: 0,
              });
            }
          } catch (error) {
            console.error('[fetchRoomDetail] Failed to get video URL:', error);
          }
        }

        // 방장이면 enterRoom API 호출 없이 바로 입장
        if (userInfo?.memberId === response.data.data.owner_id) {
          setIsEntered(true);
        } else {
          // 방장이 아니면 비밀번호 확인
          if (response.data.data.has_password) {
            // 비밀번호가 있는 방이면 모달 표시
            setIsPasswordModalOpen(true);
          } else {
            // 비밀번호가 없는 방이면 바로 입장
            try {
              await enterRoom(Number(roomId), {});

              // 입장 후 최신 방 정보 다시 가져오기 (members 업데이트)
              const updatedResponse = await getRoomDetail(Number(roomId));
              if (updatedResponse.data.success && updatedResponse.data.data) {
                setRoomData(updatedResponse.data.data);

                // 업데이트된 멤버 목록 저장 및 준비 상태 동기화
                previousMembersRef.current = updatedResponse.data.data.members.map(m => ({
                  member_id: m.member_id,
                  nickname: m.nickname,
                }));

                // 멤버들의 준비 상태를 participantsReady에 반영
                const initialReadyState: Record<number, boolean> = {};
                updatedResponse.data.data.members.forEach(m => {
                  initialReadyState[m.member_id] = m.is_ready;
                });
                setParticipantsReady(initialReadyState);
              }

              setIsEntered(true);
            } catch (error) {
              console.error('Failed to enter room:', error);
              alert('방 입장에 실패했습니다.');
              navigate('/');
            }
          }
        }
      } catch (error) {
        console.error('Failed to fetch room detail:', error);
        alert('방 정보를 가져오는데 실패했습니다.');
        navigate('/');
      } finally {
        setIsRoomLoading(false);
      }
    };

    fetchRoomDetail();
  }, [roomId, navigate, setRoomData, userInfo?.memberId]);

  // 대본 데이터는 WebSocket ROLES_CONFIRMED 메시지로 받아서 roleSegments에 저장됨

  // 카운트다운 처리
  useEffect(() => {
    if (countdown !== null && countdown > 0) {
      const timer = setTimeout(() => {
        setCountdown(countdown - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else if (countdown === 0) {
      // 카운트다운 끝나면 영상 재생
      setCountdown(null);
      if (videoRef.current) {
        videoRef.current.play();
        setIsPlaying(true);
      }
    }
  }, [countdown, roomId]);

  // Round 시작 카운트다운 처리 및 영상 재생
  useEffect(() => {
    if (timeUntilStart !== null && timeUntilStart > 0) {
      const timer = setTimeout(() => {
        setTimeUntilStart(timeUntilStart - 1);
      }, 1000);
      return () => clearTimeout(timer);
    } else if (timeUntilStart === 0) {
      // 카운트다운이 0이 되면 영상 재생
      setTimeUntilStart(null);
      setIsPlaying(true); // 먼저 isPlaying을 true로 설정하여 video 요소 렌더링

      // 다음 프레임에서 video.play() 호출 (video 요소가 렌더링된 후)
      setTimeout(() => {
        console.log('Attempting to play video, videoRef.current:', videoRef.current);
        if (videoRef.current) {
          console.log('Starting video playback for Round', currentRound);
          videoRef.current.currentTime = 0;
          videoRef.current.play().then(() => {
            console.log('Video playing successfully');
          }).catch((error) => {
            console.error('Failed to play video:', error);
          });
        } else {
          console.error('Video ref is still null after rendering');
        }
      }, 50); // 50ms 대기하여 렌더링 완료 보장
    }
  }, [timeUntilStart, currentRound]);

  // 영상 시간에 따른 자막 업데이트 (WATCHING 및 Round 모드 공통)
  useEffect(() => {
    const video = videoRef.current;
    if (!video || timeIndexedSubtitlesRef.current.length === 0) {
      return;
    }

    console.log('Setting up subtitle update');

    const updateSubtitle = () => {
      const currentTime = video.currentTime;
      const subtitles: SubtitleItem[] = [];

      // 현재 재생 중인 문장들 찾기
      const activeIndices: number[] = [];
      timeIndexedSubtitlesRef.current.forEach((sub, index) => {
        if (currentTime >= sub.start_time && currentTime <= sub.end_time) {
          activeIndices.push(index);
        }
      });

      // 각 활성 문장에 대해 이전/현재/다음 문장 추가
      const addedSentences = new Set<number>(); // 중복 방지

      if (activeIndices.length > 0) {
        // 활성 문장이 있는 경우 - 기존 로직
        activeIndices.forEach((currentIndex) => {
          const allSubtitles = timeIndexedSubtitlesRef.current;

          // 이전 문장 (같은 역할 또는 다른 역할)
          if (currentIndex > 0 && !addedSentences.has(currentIndex - 1)) {
            const prevSub = allSubtitles[currentIndex - 1];
            const userInfo = getNicknameByRoleId(prevSub.role_id);
            addedSentences.add(currentIndex - 1);
            subtitles.push({
              roleName: prevSub.role_name,
              roleId: prevSub.role_id,
              text: selectedNationality === "KR" ? prevSub.text_ko : prevSub.text_vn,
              isMyRole: currentRound >= 1 && prevSub.role_id === mySelectedRoleId,
              timing: 'prev',
              nickname: userInfo?.nickname,
              nicknameColor: userInfo ? generateUserColor(userInfo.memberId) : undefined,
            });
          }

          // 현재 문장
          if (!addedSentences.has(currentIndex)) {
            const currentSub = allSubtitles[currentIndex];
            const userInfo = getNicknameByRoleId(currentSub.role_id);
            addedSentences.add(currentIndex);
            subtitles.push({
              roleName: currentSub.role_name,
              roleId: currentSub.role_id,
              text: selectedNationality === "KR" ? currentSub.text_ko : currentSub.text_vn,
              isMyRole: currentRound >= 1 && currentSub.role_id === mySelectedRoleId,
              timing: 'current',
              nickname: userInfo?.nickname,
              nicknameColor: userInfo ? generateUserColor(userInfo.memberId) : undefined,
            });
          }

          // 다음 문장
          if (currentIndex < allSubtitles.length - 1 && !addedSentences.has(currentIndex + 1)) {
            const nextSub = allSubtitles[currentIndex + 1];
            const userInfo = getNicknameByRoleId(nextSub.role_id);
            addedSentences.add(currentIndex + 1);
            subtitles.push({
              roleName: nextSub.role_name,
              roleId: nextSub.role_id,
              text: selectedNationality === "KR" ? nextSub.text_ko : nextSub.text_vn,
              isMyRole: currentRound >= 1 && nextSub.role_id === mySelectedRoleId,
              timing: 'next',
              nickname: userInfo?.nickname,
              nicknameColor: userInfo ? generateUserColor(userInfo.memberId) : undefined,
            });
          }
        });
      } else {
        // 활성 문장이 없는 경우 - 가장 가까운 이전/다음 문장 표시
        const allSubtitles = timeIndexedSubtitlesRef.current;

        // 가장 가까운 이전 문장 찾기 (방금 끝난 문장)
        let prevIndex = -1;
        for (let i = allSubtitles.length - 1; i >= 0; i--) {
          if (allSubtitles[i].end_time <= currentTime) {
            prevIndex = i;
            break;
          }
        }

        // 가장 가까운 다음 문장 찾기 (곧 시작될 문장)
        let nextIndex = -1;
        for (let i = 0; i < allSubtitles.length; i++) {
          if (allSubtitles[i].start_time > currentTime) {
            nextIndex = i;
            break;
          }
        }

        // 이전 문장 추가
        if (prevIndex >= 0) {
          const prevSub = allSubtitles[prevIndex];
          const userInfo = getNicknameByRoleId(prevSub.role_id);
          subtitles.push({
            roleName: prevSub.role_name,
            roleId: prevSub.role_id,
            text: selectedNationality === "KR" ? prevSub.text_ko : prevSub.text_vn,
            isMyRole: currentRound >= 1 && prevSub.role_id === mySelectedRoleId,
            timing: 'prev',
            nickname: userInfo?.nickname,
            nicknameColor: userInfo ? generateUserColor(userInfo.memberId) : undefined,
          });
        }

        // 다음 문장 추가
        if (nextIndex >= 0) {
          const nextSub = allSubtitles[nextIndex];
          const userInfo = getNicknameByRoleId(nextSub.role_id);
          subtitles.push({
            roleName: nextSub.role_name,
            roleId: nextSub.role_id,
            text: selectedNationality === "KR" ? nextSub.text_ko : nextSub.text_vn,
            isMyRole: currentRound >= 1 && nextSub.role_id === mySelectedRoleId,
            timing: 'next',
            nickname: userInfo?.nickname,
            nicknameColor: userInfo ? generateUserColor(userInfo.memberId) : undefined,
          });
        }
      }

      setCurrentSubtitles(subtitles);
    };

    video.addEventListener('timeupdate', updateSubtitle);
    return () => video.removeEventListener('timeupdate', updateSubtitle);
  }, [mySelectedRoleId, isPlaying, currentRound, selectedNationality, selectedRoles, roomData]);

  // 드롭다운 외부 클릭 감지
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (layoutDropdownRef.current && !layoutDropdownRef.current.contains(event.target as Node)) {
        setIsLayoutDropdownOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  // 테마 설명 가져오기
  useEffect(() => {
    const fetchThemeDescription = async () => {
      if (!roomData?.theme_id) return;

      try {
        const response = await getThemes();
        if (response.data.success && response.data.data) {
          const theme = response.data.data.find(t => t.theme_id === roomData.theme_id);
          if (theme) {
            setThemeDescription(theme.description);
          }
        }
      } catch (error) {
        console.error('Failed to fetch theme description:', error);
      }
    };

    fetchThemeDescription();
  }, [roomData?.theme_id]);

  // 테마별 컨텐츠 목록 불러오기
  useEffect(() => {
    const fetchContents = async () => {
      if (!roomData?.theme_id) return;

      setIsContentsLoading(true);
      try {
        const response = await getThemeContents(roomData.theme_id);
        if (response.data.success && response.data.data) {
          setAvailableContents(response.data.data);
          // 캐러셀 초기 순서 설정 (메인 제외한 나머지)
          const initialOrder = response.data.data
            .map((_, index) => index)
            .filter(index => index !== 0);
          setCarouselOrder(initialOrder);
        }
      } catch (error) {
        console.error('Failed to fetch contents:', error);
      } finally {
        setIsContentsLoading(false);
      }
    };

    fetchContents();
  }, [roomData?.theme_id]);

  // 방 정보 (store에서 가져오기)
  const roomInfo = roomData ? {
    isLocked: roomData.has_password,
    title: roomData.title,
    password: '',
    themeId: roomData.theme_id,
    themeName: roomData.theme_name
  } : {
    isLocked: false,
    title: "Loading...",
    password: "",
    themeId: 1,
    themeName: ""
  };

  // 메인 컨텐츠 전환 핸들러 (캐러셀에서 선택 시)
  const handleMainContentChange = (newMainIndex: number) => {
    if (isTransitioning || newMainIndex === mainContentIndex) return;

    setIsTransitioning(true);

    // 원래 메인 컨텐츠를 캐러셀 마지막에 추가
    const oldMainIndex = mainContentIndex;
    setCarouselOrder(prev => [...prev.filter(idx => idx !== newMainIndex), oldMainIndex]);

    // 메인 컨텐츠 변경
    setMainContentIndex(newMainIndex);

    // 애니메이션 완료 후 상태 초기화
    setTimeout(() => {
      setIsTransitioning(false);
    }, 700);
  };

  const handleContentSelect = async (content: Content) => {
    if (!roomId) return;

    // 컨텐츠 선택 확정 애니메이션 시작
    setIsTransitioning(true);

    try {
      // 방 컨텐츠 선택 API 호출 (실제 content_id 전달)
      // 서버가 CONTENT_SELECTED 메시지를 모든 사용자에게 브로드캐스트함
      const response = await selectRoomContent(Number(roomId), content.content_id);

      if (response.data.success) {
        // content_id와 videoUrl은 WebSocket CONTENT_SELECTED 메시지에서 처리됨
        setSelectedContent(content);
        setIsContentSelectOpen(false);
        // 컨텐츠 선택 시 모든 참가자의 준비 상태 초기화
        setIsReady(false);
        setParticipantsReady({});

        // 애니메이션 완료 후 상태 초기화
        setTimeout(() => {
          setIsTransitioning(false);
        }, 700);
      } else {
        setToastMessage('컨텐츠 선택에 실패했어요');
        setIsTransitioning(false);
      }
    } catch (error) {
      console.error('Failed to select content:', error);
      setToastMessage('컨텐츠 선택에 실패했어요');
      setIsTransitioning(false);
    }
  };

  const handleCancelContentSelection = () => {
    if (!selectedContent) return;

    // 선택된 컨텐츠를 availableContents에서 찾아서 mainContentIndex로 설정
    const contentIndex = availableContents.findIndex(
      (content) => content.content_id === selectedContent.content_id
    );

    if (contentIndex !== -1) {
      setMainContentIndex(contentIndex);
      // 캐러셀 순서 업데이트
      const newCarouselOrder = availableContents
        .map((_, index) => index)
        .filter(index => index !== contentIndex);
      setCarouselOrder(newCarouselOrder);
    }

    // 상태 초기화
    setSelectedContent(null);
    setVideoUrl(null);
    setIsReady(false);
    setParticipantsReady({});
  };

  const handleToggleReady = () => {
    if (isReadyLoading) return; // 이미 처리 중이면 무시

    setIsReadyLoading(true);

    // 기존 타임아웃 클리어
    if (readyTimeoutRef.current) {
      clearTimeout(readyTimeoutRef.current);
    }

    // 타임아웃 설정 (5초 후 응답 없으면 실패 처리)
    readyTimeoutRef.current = setTimeout(() => {
      setIsReadyLoading(false);
      setToastMessage('준비 완료에 실패했어요');
    }, 5000);

    // WebSocket으로 준비 상태 전송
    try {
      wsToggleReady(!isReady);
    } catch (error) {
      if (readyTimeoutRef.current) {
        clearTimeout(readyTimeoutRef.current);
      }
      setIsReadyLoading(false);
      setToastMessage('준비 완료에 실패했어요');
      console.error('Failed to toggle ready:', error);
    }
  };

  const handleStartShadowing = async () => {
    if (!roomId || !contentId) {
      setToastMessage('컨텐츠를 선택해주세요');
      return;
    }

    try {
      setIsGameStarting(true); // 게임 시작 중 상태로 변경

      // 준비 상태 초기화
      setIsReady(false);
      setParticipantsReady({});

      // 방장이 게임 시작 API 호출
      const response = await startGame(Number(roomId), { content_id: contentId });

      if (response.data.success) {
        // WebSocket에서 phase가 WATCHING으로 변경되면 카운트다운 시작
      } else {
        setIsGameStarting(false);
        setToastMessage('게임 시작에 실패했어요');
      }
    } catch (error) {
      console.error('Failed to start game:', error);
      setIsGameStarting(false);
      setToastMessage('게임 시작에 실패했어요');
    }
  };

  const handleStartRound = async (round: number) => {
    if (!roomId) return;

    try {
      setIsRoundStarting(true);
      const response = await startRound(Number(roomId), { round });

      if (response.data.success) {
        console.log(`Round ${round} start API called successfully`);
        // WebSocket ROUND_START 메시지를 기다림 (실제 영상 재생은 그때 처리)
      } else {
        setIsRoundStarting(false);
        setToastMessage('라운드 시작에 실패했어요');
      }
    } catch (error) {
      console.error('Failed to start round:', error);
      setIsRoundStarting(false);
      setToastMessage('라운드 시작에 실패했어요');
    }
  };

  const handleRoleSelect = (role: Role) => {
    // WebSocket으로 역할 선택 메시지 전송
    assignRole(role.role_id);
    console.log('Role selected:', role);
  };

  const handleConfirmRoles = async () => {
    if (!roomId) return;

    try {
      setIsConfirmingRoles(true);
      const confirmData = getConfirmData();

      console.log('Confirming roles:', confirmData);
      const response = await confirmRoles(Number(roomId), { roles: confirmData });

      if (response.data.success) {
        setToastMessage('역할 선택이 확정되었습니다');
        setIsRoleSelectOpen(false);
        setIsRoleAssigned(true); // 역할 확정 완료 - 이제 캐릭터 선택 버튼 숨김
      } else {
        setToastMessage('역할 확정에 실패했어요');
      }
    } catch (error) {
      console.error('Failed to confirm roles:', error);
      setToastMessage('역할 확정에 실패했어요');
    } finally {
      setIsConfirmingRoles(false);
    }
  };

  // 상태 초기화 함수
  const resetToWaitingState = () => {
    console.log('Resetting to waiting state');
    setIsRoleAssigned(false);
    setCurrentRound(0);
    setIsRoundInProgress(false);
    setIsGameStarting(false);
    setIsRoundStarting(false);
    setIsFinishingRound(false);
    setIsReady(false);
    setSelectedContent(null);
    setVideoUrl(null);

    // 역할 정보 초기화
    clearRoles();

    setToastMessage('대기 상태로 돌아갔습니다');
  };

  const handleFinishRoom = async () => {
    if (!roomId) return;

    try {
      console.log('Finishing room...');
      const response = await finishRoom(Number(roomId));

      if (response.data.success) {
        console.log('Room finished successfully');
        // WebSocket PHASE_CHANGE (WAITING) 메시지를 기다림
      } else {
        setToastMessage('처음으로 돌아가기에 실패했어요');
      }
    } catch (error) {
      console.error('Failed to finish room:', error);
      setToastMessage('처음으로 돌아가기에 실패했어요');
    }
  };

  // WebRTC 연결
  const {
    status,
    error,
    tiles,
    publisher,
    isAudioEnabled,
    isVideoEnabled,
    join,
    leave,
    publishStream,
    toggleAudio,
    toggleVideo,
  } = useVideoRoom({
    roomId: Number(roomId),
    nickname,
    password: undefined,
    autoJoin: false,
    autoPublish: false, // 세팅 완료 후 수동으로 publish
    isOwner: isOwner
  });

  // leave 함수의 안정적 참조 (useEffect deps 재실행 방지)
  const leaveRef = useRef(leave);
  useEffect(() => {
    leaveRef.current = leave;
  }, [leave]);

  const toggleSubtitle = () => setIsSubtitleEnabled(!isSubtitleEnabled);

  // 쉐도잉 영상 볼륨 적용
  useEffect(() => {
    if (videoRef.current) {
      videoRef.current.volume = volume / 100;
    }
  }, [volume]);

  // Round 모드: 영상 소리 자동 음소거/해제 (기본 음소거, 할당되지 않은 구간만 ±500ms 음소거 해제)
  useEffect(() => {
    const video = videoRef.current;
    if (!video || !isPlaying || currentRound < 1 || roleSegments.length === 0) {
      return;
    }

    console.log('Setting up video mute control for unassigned sentences');

    // 할당된 문장의 sentence_id들을 Set으로 수집
    const assignedSentenceIds = new Set<number>();
    roleSegments.forEach((segment) => {
      segment.sentences.forEach((sentence: Sentence) => {
        assignedSentenceIds.add(sentence.sentence_id);
      });
    });

    // 할당되지 않은 문장들의 시간 범위 (±500ms) 계산
    const unassignedTimeRanges: Array<{ start: number; end: number }> = [];
    timeIndexedSubtitlesRef.current.forEach((subtitle) => {
      if (!assignedSentenceIds.has(subtitle.sentence_id)) {
        unassignedTimeRanges.push({
          start: subtitle.start_time - 0.5, // 500ms 전
          end: subtitle.end_time + 0.5,     // 500ms 후
        });
      }
    });

    // 시작 시간 기준으로 정렬
    unassignedTimeRanges.sort((a, b) => a.start - b.start);

    const handleTimeUpdate = () => {
      const currentTime = video.currentTime;

      // 현재 시간이 할당되지 않은 구간의 음성 시간대(±500ms)에 속하는지 확인
      const isUnassignedVoiceTime = unassignedTimeRanges.some(
        (range) => currentTime >= range.start && currentTime <= range.end
      );

      // 기본적으로 음소거, 할당되지 않은 구간의 음성 시간대만 음소거 해제
      video.muted = !isUnassignedVoiceTime;
    };

    video.addEventListener('timeupdate', handleTimeUpdate);
    return () => {
      video.removeEventListener('timeupdate', handleTimeUpdate);
      // 클린업 시 음소거 해제
      if (video) {
        video.muted = false;
      }
    };
  }, [isPlaying, currentRound, roleSegments]);

  // 녹음 스케줄링 + 마이크 자동 음소거 (Round 진행 중, 내 역할의 문장에 대해서만)
  useEffect(() => {
    const video = videoRef.current;
    if (!videoReady || !video || !isPlaying || currentRound < 1 || roleSegments.length === 0 || !mySelectedRoleId) {
      return;
    }

    console.log('Setting up recording schedule and mic control for Round', currentRound);

    const timeouts: number[] = [];

    // 내 역할의 세그먼트 찾기
    const mySegment = roleSegments.find(seg => seg.role_id === mySelectedRoleId);
    if (!mySegment) {
      console.log('No segment found for my role');
      return;
    }

    // Round 시작 시 마이크 음소거 (내 차례가 아닐 때 기본 상태)
    if (publisher && isAudioEnabled) {
      publisher.publishAudio(false);
      console.log('Mic muted at round start (not my turn)');
    }

    // 각 문장에 대해 녹음 스케줄링 + 마이크 제어
    mySegment.sentences.forEach((sentence: Sentence) => {
      const startTime = (sentence.start_time - 0.5) * 1000; // 500ms 전
      const endTime = (sentence.end_time + 0.5) * 1000; // 500ms 후

      // 녹음 시작 타이머 (+ 마이크 켜기)
      const startTimeout = setTimeout(async () => {
        try {
          // 내 차례 시작: 마이크 켜기 (사용자가 수동으로 끈 경우 제외)
          if (publisher && isAudioEnabled) {
            publisher.publishAudio(true);
            console.log(`Mic unmuted for my turn (sentence ${sentence.sentence_id})`);
          }

          console.log(`Getting presigned URL for sentence ${sentence.sentence_id}`);

          // Presigned URL 받기
          const response = await getPresignedUrl({
            room_id: Number(roomId),
            round: currentRound,
            member_id: memberId,
            sentence_id: sentence.sentence_id,
          });

          if (response.data.success && response.data.data) {
            const { upload_url, s3_key } = response.data.data;
            presignedUrlsRef.current.set(sentence.sentence_id, upload_url);
            s3KeysRef.current.set(sentence.sentence_id, s3_key);
            console.log(`Presigned URL received for sentence ${sentence.sentence_id}, s3_key: ${s3_key}`);

            // 녹음 시작
            console.log(`Starting recording for sentence ${sentence.sentence_id} at ${sentence.start_time - 0.5}s`);
            currentRecordingSentenceIdRef.current = sentence.sentence_id;
            startRecordingRef.current();
          } else {
            console.error('Failed to get presigned URL:', response.data.error);
          }
        } catch (error) {
          console.error('Failed to get presigned URL:', error);
        }
      }, startTime);

      // 녹음 종료 타이머 (+ 마이크 끄기)
      const endTimeout = setTimeout(() => {
        console.log(`Stopping recording for sentence ${sentence.sentence_id} at ${sentence.end_time + 0.5}s`);
        stopRecordingRef.current();
        // sentenceId는 onRecordingComplete에서 초기화 (비동기 onstop 이벤트 이후)

        // 내 차례 종료: 마이크 음소거
        if (publisher) {
          publisher.publishAudio(false);
          console.log(`Mic muted after my turn (sentence ${sentence.sentence_id})`);
        }
      }, endTime);

      timeouts.push(startTimeout, endTimeout);
    });

    // 클린업
    return () => {
      console.log('Cleaning up recording schedule and mic control');
      timeouts.forEach(timeout => clearTimeout(timeout));
      stopRecordingRef.current();

      // Round 종료 시 마이크 원래 상태로 복원
      if (publisher && isAudioEnabled) {
        publisher.publishAudio(true);
        console.log('Mic restored to original state');
      }
    };
  }, [videoReady, isPlaying, currentRound, roleSegments, mySelectedRoleId, roomId, memberId, publisher, isAudioEnabled]);

  // 비밀번호 입력 후 방 입장
  const handlePasswordSubmit = async () => {
    if (!roomId || !password.trim()) {
      setPasswordError('비밀번호를 입력해주세요');
      return;
    }

    try {
      setPasswordError('');
      await enterRoom(Number(roomId), { password });

      // 입장 후 최신 방 정보 다시 가져오기 (members 업데이트)
      const updatedResponse = await getRoomDetail(Number(roomId));
      if (updatedResponse.data.success && updatedResponse.data.data) {
        setRoomData(updatedResponse.data.data);

        // 업데이트된 멤버 목록 저장 및 준비 상태 동기화
        previousMembersRef.current = updatedResponse.data.data.members.map(m => ({
          member_id: m.member_id,
          nickname: m.nickname,
        }));

        // 멤버들의 준비 상태를 participantsReady에 반영
        const initialReadyState: Record<number, boolean> = {};
        updatedResponse.data.data.members.forEach(m => {
          initialReadyState[m.member_id] = m.is_ready;
        });
        setParticipantsReady(initialReadyState);
      }

      setIsEntered(true);
      setIsPasswordModalOpen(false);
      setPassword('');
    } catch (error) {
      console.error('Failed to enter room:', error);
      const errorMessage = (error as { response?: { data?: { error?: { message?: string } } } })?.response?.data?.error?.message || '방 입장에 실패했습니다.';
      setPasswordError(errorMessage);
    }
  };

  // 모든 참가자가 준비 완료되었는지 확인
  // getRoomDetail의 members 배열 기반으로 인원 수 계산 (WebRTC와 분리)
  const totalParticipants = roomData?.members.length || 0;
  const readyCount = Object.values(participantsReady).filter(ready => ready).length;
  const allParticipantsReady = totalParticipants > 0 && readyCount === totalParticipants;

  const handleAudioDeviceChange = (deviceId: string) => {
    setSelectedAudioDevice(deviceId);
    // TODO: 실제 구현시 미디어 스트림 변경 로직 추가
    console.log('Audio device changed to:', deviceId);
  };

  const handleVideoDeviceChange = (deviceId: string) => {
    setSelectedVideoDevice(deviceId);
    // TODO: 실제 구현시 미디어 스트림 변경 로직 추가
    console.log('Video device changed to:', deviceId);
  };

  // 방 입장 후 미디어 체크 완료 시 WebRTC 연결 (publisher 생성, 아직 publish 안 함)
  useEffect(() => {
    // WebRTC 비활성화 시 연결하지 않음
    if (DISABLE_WEBRTC) return;

    if (isEntered && roomId && status === 'idle' && isMediaChecked) {
      join();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isEntered, roomId, status, isMediaChecked]);

  // WebRTC 연결 완료 후 스트림 publish (다른 사람들에게 보이기 시작)
  useEffect(() => {
    // WebRTC 비활성화 시 실행하지 않음
    if (DISABLE_WEBRTC) return;

    if (status === 'connected' && publisher && isMediaChecked) {
      // 100ms 지연 후 publishStream 호출 (안정성 확보)
      const timer = setTimeout(() => {
        console.log('Publishing stream after connection established');
        publishStream();
      }, 100);

      return () => clearTimeout(timer);
    }
  }, [status, publisher, isMediaChecked, publishStream]);

  // 브라우저 뒤로가기 감지 및 처리
  useEffect(() => {
    const handlePopState = async () => {
      if (roomId && isEntered && !isLeaving) {
        // 뒤로가기 방지 (일단 현재 위치 유지)
        window.history.pushState(null, '', window.location.href);

        // leave 완료 후 이동
        await handleLeaveInternal();
      }
    };

    // 초기 진입 시 히스토리 스택에 현재 위치 추가 (뒤로가기 감지용)
    window.history.pushState(null, '', window.location.href);
    window.addEventListener('popstate', handlePopState);

    return () => {
      window.removeEventListener('popstate', handlePopState);
    };
  }, [roomId, isEntered, isLeaving]);

  // 방 퇴장 처리 (컴포넌트 언마운트 시 - 강제 종료 대비)
  useEffect(() => {
    return () => {
      // useVideoRoom의 cleanup에서 미디어 트랙 정리가 이미 처리됨
      // 여기서는 추가 정리 작업 없음
    };
  }, []);

  // 브라우저 닫기/새로고침 시 퇴장 처리
  useEffect(() => {
    const handleBeforeUnload = () => {
      // useVideoRoom의 cleanup에서 미디어 트랙 정리가 자동으로 처리됨
      // 백엔드는 WebSocket 연결 해제로 자동 정리됨
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, []);

  if (!roomId) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4 bg-gray-50">
        <p className="text-red-600 text-lg font-medium">유효하지 않은 방 ID입니다</p>
        <button
          onClick={() => navigate("/")}
          className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
        >
          홈으로 돌아가기
        </button>
      </div>
    );
  }

  // 내부 leave 처리 함수 (뒤로가기/나가기 버튼 공통)
  const handleLeaveInternal = async () => {
    if (isLeaving) return; // 이미 처리 중이면 무시

    setIsLeaving(true);
    setIsEntered(false); // useEffect 재실행 방지

    try {
      if (roomId) {
        // 1. OpenVidu 세션 정리 (카메라/마이크 즉시 종료)
        await leaveRef.current();

        // 2. WebRTC 퇴장 API 호출 (완료 대기)
        try {
          await leaveWebRTC(Number(roomId));
        } catch (error) {
          console.error('Failed to leave WebRTC:', error);
          // WebRTC leave 실패해도 계속 진행
        }

        // TODO: TimeOut 시간 제한

        // 3. 방 퇴장 API 호출 (완료 대기)
        await leaveRoom(Number(roomId));

        // 4. 로컬 데이터 정리
        clearRoomData();
        clearRoles(); // 역할 정보 초기화
      }

      // 5. 모든 정리 완료 후 홈으로 이동
      navigate("/", { replace: true });
    } catch (error) {
      console.error('Failed to leave room:', error);
      // 에러 발생해도 페이지 이동
      navigate("/", { replace: true });
    } finally {
      setIsLeaving(false);
    }
  };

  // 나가기 버튼 클릭 핸들러
  const handleLeave = () => {
    handleLeaveInternal();
  };

  const handleCopyPassword = async () => {
    try {
      await navigator.clipboard.writeText(roomInfo.password);
      setCopiedPassword(true);
      setTimeout(() => setCopiedPassword(false), 2000);
    } catch (err) {
      console.error("Failed to copy password:", err);
    }
  };

  const handleLayoutChange = (mode: LayoutMode) => {
    setLayoutMode(mode);
    setIsLayoutDropdownOpen(false);
  };

  const layoutConfigs = {
    narrow: { width: "w-90", label: "1열 (기본)", icon: LayoutList },
    grid: { width: "w-[600px]", label: "2x2 그리드", icon: LayoutGrid },
    wide: { width: "w-[480px]", label: "넓은 사이드바", icon: Maximize2 },
  };

  const handleMediaCheckComplete = (
    audioEnabled: boolean,
    videoEnabled: boolean,
    audioDeviceId?: string,
    videoDeviceId?: string
  ) => {
    setIsMediaChecked(true);

    // 선택된 장치 정보 저장
    setSelectedAudioDevice(audioDeviceId);
    setSelectedVideoDevice(videoDeviceId);

    // 오디오/비디오 설정 반영
    if (publisher) {
      publisher.publishAudio(audioEnabled);
      publisher.publishVideo(videoEnabled);
    }

    // useVideoRoom 상태 동기화 (VideoControls 반영용)
    // 초기값이 true이므로 false인 경우만 토글
    if (!audioEnabled && isAudioEnabled) {
      toggleAudio();
    }
    if (!videoEnabled && isVideoEnabled) {
      toggleVideo();
    }

    // publishStream() 호출 제거 - useEffect에서 status === 'connected'일 때 자동 호출됨
  };

  // 방 정보 로딩 중
  if (isRoomLoading) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4 bg-gray-50">
        <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-gray-600">방 정보를 불러오는 중...</p>
      </div>
    );
  }

  // 입장 전
  if (!isEntered) {
    return (
      <div className="flex flex-col items-center justify-center min-h-screen gap-4 bg-gray-50">
        <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
        <p className="text-gray-600">입장 중...</p>
      </div>
    );
  }

  return (
    <div className="flex h-screen bg-gray-50 relative">
      {/* 왼쪽 메인 영역 */}
      <div className="flex flex-1 flex-col">
        {/* 헤더 */}
        <div className="w-full border-b border-gray-200 px-6 py-6 bg-white">
          <div className="flex items-center gap-3">
            {/* 잠금 아이콘 */}
            <div className={`p-2 rounded-lg ${roomInfo.isLocked ? "bg-yellow-50" : "bg-green-50"}`}>
              {roomInfo.isLocked ? (
                <Lock size={20} className="text-yellow-600" />
              ) : (
                <Unlock size={20} className="text-green-600" />
              )}
            </div>

            {/* 방 제목 */}
            <h1 className="text-lg font-semibold text-gray-900 flex-1">
              {roomInfo.title}
            </h1>

            {/* 비밀번호 복사 버튼 */}
            {roomInfo.isLocked && (
              <button
                onClick={handleCopyPassword}
                className="flex items-center gap-2 px-3 py-2 bg-gray-100 hover:bg-gray-200 rounded-lg transition-colors"
              >
                {copiedPassword ? (
                  <>
                    <Check size={16} className="text-blue-600" />
                    <span className="text-sm text-blue-600">복사됨</span>
                  </>
                ) : (
                  <>
                    <Copy size={16} className="text-gray-600" />
                    <span className="text-sm text-gray-700">비밀번호 복사</span>
                  </>
                )}
              </button>
            )}
          </div>
        </div>

        {/* 메인 비디오 영역 */}
        <div className="flex-1 p-4 bg-white">
          <div className="relative h-full w-full rounded-lg bg-black flex items-center justify-center overflow-hidden">
            {/* WebRTC 비활성화 시 연결 상태 무시 */}
            {!DISABLE_WEBRTC && status === "connecting" && (
              <div className="flex flex-col items-center gap-3">
                <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
                <p className="text-gray-400">연결 중...</p>
              </div>
            )}
            {!DISABLE_WEBRTC && status === "error" && (
              <div className="flex flex-col items-center gap-3">
                <p className="text-red-500">{error}</p>
                <button
                  onClick={join}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
                >
                  다시 시도
                </button>
              </div>
            )}
            {(DISABLE_WEBRTC || status === "connected") && (
              <>
                {/* 카운트다운 오버레이 */}
                {countdown !== null && (
                  <div className="absolute inset-0 flex items-center justify-center bg-black/70 z-20 rounded-lg">
                    <div className="text-white text-9xl font-bold animate-pulse">
                      {countdown}
                    </div>
                  </div>
                )}

                {/* 라운드 종료 처리 중 오버레이 */}
                {isFinishingRound && (
                  <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/50 backdrop-blur-sm z-30 rounded-lg">
                    <div className="w-16 h-16 border-4 border-white/30 border-t-white rounded-full animate-spin" />
                    <p className="text-white text-xl font-semibold mt-4">
                      라운드 종료 처리 중...
                    </p>
                  </div>
                )}

                {/* Round 시작 대기 오버레이 */}
                {timeUntilStart !== null && timeUntilStart > 0 && (
                  <div className="absolute inset-0 flex flex-col items-center justify-center bg-black/80 z-20 rounded-lg">
                    <div className="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin mb-4" />
                    <p className="text-white text-2xl font-semibold mb-2">
                      Round {currentRound} 준비 중
                    </p>
                    <p className="text-gray-300 text-lg">
                      {timeUntilStart}초 후 시작
                    </p>
                  </div>
                )}

                {/* 영상 재생 중 */}
                {isPlaying && selectedContent && videoUrl && (
                  <div className="relative w-full h-full flex items-center justify-center overflow-hidden">
                    <video
                      ref={videoRef}
                      src={videoUrl}
                      className="absolute inset-0 w-full h-full object-contain"
                      autoPlay
                      playsInline
                      onContextMenu={(e) => e.preventDefault()}
                      style={{ pointerEvents: 'none' }}
                      onLoadedMetadata={() => setVideoReady(true)}
                      onPlay={() => setVideoReady(true)}
                      onCanPlay={() => {
                        // 비디오 메타데이터 로드 완료 후 재생
                        if (needPlayOnCanPlayRef.current && videoRef.current) {
                          console.log('onCanPlay: Playing video');
                          needPlayOnCanPlayRef.current = false; // 한 번만 실행되도록
                          videoRef.current.play().catch(err => {
                            console.error('onCanPlay video play failed:', err);
                          });
                        }
                      }}
                      onEnded={async () => {
                        // 영상 재생 완료 시 처리
                        setIsPlaying(false);

                        // Round 진행 중이면 라운드 종료
                        if (currentRound >= 1 && isRoundInProgress) {
                          setIsRoundInProgress(false);
                          setToastMessage(`Round ${currentRound} 완료`);

                          // 방장만 2초 후 finishRound API 호출
                          if (isOwner && roomId) {
                            setIsFinishingRound(true);
                            setTimeout(async () => {
                              try {
                                const response = await finishRound(Number(roomId), currentRound);
                                if (!response.data.success) {
                                  console.error('Failed to finish round:', response.data.error?.message);
                                  setToastMessage('라운드 종료에 실패했어요');
                                }
                              } catch (error) {
                                console.error('Failed to finish round:', error);
                                setToastMessage('라운드 종료에 실패했어요');
                              } finally {
                                setIsFinishingRound(false);
                              }
                            }, 2000);
                          }
                        } else if (roomId && isOwner && contentId) {
                          // 첫 번째 시청 완료 시 finishWatching API 호출 (방장만)
                          setIsWaitingForRolePick(true); // 역할 선택 대기 시작
                          try {
                            const response = await finishWatching(Number(roomId), { content_id: contentId });
                            if (response.data.success) {
                              setIsGameStarting(false);
                            } else {
                              console.error('Failed to finish watching:', response.data.error?.message);
                            }
                          } catch (error) {
                            console.error('Failed to finish watching:', error);
                          }
                        } else {
                          // 방장이 아닌 경우에도 대기 상태로 전환
                          setIsWaitingForRolePick(true);
                        }
                      }}
                    />

                    {/* Gradient Overlay */}
                    <div className="absolute inset-0 bg-gradient-to-t from-black/60 via-transparent to-black/40 pointer-events-none" />

                    {/* 대본 표시 (WATCHING 및 Round 모드) */}
                    {isSubtitleEnabled && currentSubtitles.length > 0 && (
                      <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 w-full max-w-5xl px-4">
                        <div className="space-y-2">
                          {currentSubtitles.map((subtitle, index) => {
                            const isCurrent = subtitle.timing === 'current';
                            const isPrev = subtitle.timing === 'prev';
                            const isNext = subtitle.timing === 'next';

                            return (
                              <div
                                key={`${subtitle.roleId}-${subtitle.timing}-${index}`}
                                className={`px-5 py-3 rounded-lg text-center flex justify-center items-center gap-2 transition-all ${
                                  isCurrent
                                    ? 'bg-black/80'
                                    : 'bg-black/50'
                                } ${
                                  isPrev || isNext ? 'opacity-70' : 'opacity-100'
                                }`}
                              >
                                {/* 닉네임 표시 (역할을 맡은 경우만) */}
                                {subtitle.nickname && (
                                  <p
                                    className={`text-sm font-semibold ${
                                      isCurrent ? 'text-base' : 'text-xs'
                                    }`}
                                    style={{ color: subtitle.nicknameColor || '#9CA3AF' }}
                                  >
                                    {subtitle.nickname}
                                    {subtitle.isMyRole && ' (나)'}
                                  </p>
                                )}
                                {/* 자막 텍스트 */}
                                <p
                                  className={`text-white font-medium ${
                                    isCurrent
                                      ? 'text-xl'
                                      : isPrev
                                      ? 'text-base'
                                      : 'text-base'
                                  }`}
                                >
                                  {subtitle.text}
                                </p>
                              </div>
                            );
                          })}
                        </div>
                      </div>
                    )}
                  </div>
                )}

                {/* 영상 재생 전 */}
                {!isPlaying && (
                  <>
                    {/* 비디오 배경 (blur 처리) */}
                    {selectedContent && videoUrl && (
                      <div className="absolute inset-0 overflow-hidden">
                        {/* 강한 블러 배경 (전체) */}
                        <video
                          src={videoUrl}
                          className="w-full h-full object-cover"
                          style={{ filter: 'blur(20px)', transform: 'scale(1.1)' }}
                          muted
                          playsInline
                        />
                        {/* 약한 블러 가운데 (그라데이션 마스크 - 넷플릭스 스타일) */}
                        <video
                          src={videoUrl}
                          className="absolute inset-0 w-full h-full object-cover"
                          style={{
                            filter: 'blur(2px)',
                            transform: 'scale(1.1)',
                            maskImage:
                              'linear-gradient(to bottom, transparent 0%, black 20%, black 80%, transparent 100%)',
                            WebkitMaskImage:
                              'linear-gradient(to bottom, transparent 0%, black 20%, black 80%, transparent 100%)',
                          }}
                          muted
                          playsInline
                        />
                        <div className="absolute inset-0 bg-black/65" />
                      </div>
                    )}

                    {/* 컨텐츠 정보 - 왼쪽 중앙 */}
                    {selectedContent && (
                      <div className="absolute left-8 top-[35%] z-20 max-w-2xl">
                        {/* 테마 이름 */}
                        <span className="inline-block px-4 py-1.5 bg-gray-500/30 text-gray-300 text-sm font-medium rounded-full mb-3">
                          {roomInfo.themeName}
                        </span>
                        {/* 컨텐츠 제목 */}
                        <h2 className="text-white text-5xl font-bold mb-3">{selectedContent.title}</h2>
                        {/* 테마 설명 */}
                        <p className="text-gray-200 text-base leading-relaxed mb-4">{themeDescription}</p>
                        {/* 컨텐츠 선택 취소 버튼 (방장만) */}
                        {isOwner && !isPlaying && !isGameStarting && !isRoleAssigned && !isWaitingForRolePick && !isRoleSelectOpen && (
                          <button
                            onClick={handleCancelContentSelection}
                            className="flex items-center gap-2 px-4 py-2 bg-white/10 hover:bg-white/20 backdrop-blur-sm text-white rounded-lg transition-colors border border-white/30"
                          >
                            <X size={18} />
                            컨텐츠 선택 취소
                          </button>
                        )}
                      </div>
                    )}

                    {/* 역할 선택 대기 중 로딩 */}
                    {isWaitingForRolePick ? (
                      <div className="relative flex flex-col items-center gap-4 z-10">
                        <div className="w-16 h-16 border-4 border-blue-500 border-t-transparent rounded-full animate-spin" />
                        <p className="text-white text-xl font-semibold">
                          다른 사용자들을 기다리는 중입니다..
                        </p>
                      </div>
                    ) : (
                      <>
                        {!selectedContent ? (
                          isOwner ? (
                            <div className="absolute inset-0 overflow-hidden">
                              {/* 좌측 상단 타이틀 */}
                              <div
                                className={`absolute top-8 left-8 z-40 transition-all duration-700 ${
                                  isTransitioning && selectedContent ? '-translate-y-full opacity-0' : 'translate-y-0 opacity-100'
                                }`}
                              >
                                <h1 className="text-white text-2xl font-bold">컨텐츠 선택</h1>
                              </div>

                              {isContentsLoading ? (
                                <div className="flex items-center justify-center h-full">
                                  <div className="w-16 h-16 border-4 border-white/30 border-t-white rounded-full animate-spin" />
                                </div>
                              ) : availableContents.length === 0 ? (
                                <div className="flex items-center justify-center h-full text-gray-300">
                                  컨텐츠가 없습니다.
                                </div>
                              ) : (
                                <>
                                  {/* 메인 컨텐츠 배경 이미지 - 전체 화면 */}
                                  <div className="absolute inset-0 transition-opacity duration-700 ease-in-out">
                                    <img
                                      src={availableContents[mainContentIndex].thumbnail_url}
                                      alt={availableContents[mainContentIndex].title}
                                      className="absolute inset-0 w-full h-full object-cover transition-transform duration-700 ease-in-out"
                                    />
                                  </div>

                                  {/* Gradient Overlays */}
                                  {/* 양쪽 사이드 gradient */}
                                  <div className="absolute inset-y-0 left-0 w-1/4 bg-gradient-to-r from-black/60 to-transparent pointer-events-none z-10" />
                                  <div className="absolute inset-y-0 right-0 w-1/4 bg-gradient-to-l from-black/60 to-transparent pointer-events-none z-10" />

                                  {/* 하단 강한 gradient */}
                                  <div className="absolute bottom-0 left-0 right-0 h-2/3 bg-gradient-to-t from-black via-black/80 to-transparent pointer-events-none z-10" />

                                  {/* 컨텐츠 정보 */}
                                  <div className="absolute left-8 top-[35%] z-20 max-w-2xl transition-all duration-700 ease-in-out">
                                    {/* 테마 이름 */}
                                    <span className="inline-block px-4 py-1.5 bg-gray-500/30 text-gray-300 text-sm font-medium rounded-full mb-3">
                                      {roomInfo.themeName}
                                    </span>

                                    {/* 제목 */}
                                    <h2 className="text-white text-5xl font-bold mb-3">{availableContents[mainContentIndex].title}</h2>

                                    {/* 설명 */}
                                    <p className="text-gray-200 text-base leading-relaxed line-clamp-2 mb-4">{availableContents[mainContentIndex].description}</p>

                                    {/* 하단 정보 및 버튼 */}
                                    <div className="flex items-center gap-6">
                                      {/* 인원수 */}
                                      {availableContents[mainContentIndex].max_people && (
                                        <div className="flex items-center gap-2 text-white">
                                          <Users size={20} />
                                          <span className="text-base font-medium">{availableContents[mainContentIndex].max_people}명</span>
                                        </div>
                                      )}
                                      {/* 영상 길이 */}
                                      <div className="flex items-center gap-2 text-white">
                                        <Clock size={20} />
                                        <span className="text-base font-medium">{Math.floor(availableContents[mainContentIndex].total_duration / 60)}:{Math.floor(availableContents[mainContentIndex].total_duration % 60).toString().padStart(2, '0')}</span>
                                      </div>

                                      {/* 선택 버튼 */}
                                      <button
                                        onClick={() => handleContentSelect(availableContents[mainContentIndex])}
                                        className="ml-2 px-8 py-3 bg-white hover:bg-gray-100 text-gray-900 rounded-full font-semibold text-lg transition-all shadow-xl"
                                      >
                                        이 컨텐츠로 시작하기
                                      </button>
                                    </div>
                                  </div>

                                  {/* 하단 캐러셀 - 다른 컨텐츠들 (현재 메인 컨텐츠 제외) */}
                                  <div
                                    className={`absolute bottom-0 left-0 right-0 z-30 px-8 min-[1920px]:pb-8 transition-all duration-700 ${
                                      isTransitioning && selectedContent ? 'translate-y-full opacity-0' : 'translate-y-0 opacity-100'
                                    }`}
                                  >
                                    <div className="flex gap-4 overflow-x-auto pb-2 scrollbar-thin scrollbar-thumb-white/30 scrollbar-track-transparent hover:scrollbar-thumb-white/50">
                                      {carouselOrder.map((contentIndex) => {
                                        const content = availableContents[contentIndex];
                                        if (!content) return null;
                                        return (
                                          <button
                                            key={content.content_id}
                                            onClick={() => handleMainContentChange(contentIndex)}
                                            className="shrink-0 w-64 min-[1920px]:w-80 border border-white/20 hover:border-white/40 hover:shadow-lg bg-white/5 rounded-lg overflow-hidden transition-all duration-500 text-left group animate-slide-in-right"
                                          >
                                          <div className="relative w-full h-44 min-[1920px]:h-52 overflow-hidden">
                                            <img
                                              src={content.thumbnail_url}
                                              alt={content.title}
                                              className="w-full h-full object-cover transition-transform duration-300 group-hover:scale-105"
                                            />
                                            {/* 인원수 */}
                                            {content.max_people && (
                                              <div className="absolute top-2 left-2 bg-black/70 text-white px-2 py-1 rounded text-xs flex items-center gap-1">
                                                <Users size={12} />
                                                <span>{content.max_people}명</span>
                                              </div>
                                            )}
                                            {/* 영상 길이 */}
                                            <div className="absolute bottom-2 right-2 bg-black/70 text-white px-2 py-1 rounded text-xs flex items-center gap-1">
                                              <Clock size={12} />
                                              <span>{Math.floor(content.total_duration / 60)}:{Math.floor(content.total_duration % 60).toString().padStart(2, '0')}</span>
                                            </div>
                                          </div>
                                          <div className="p-4">
                                            <h3 className="font-semibold mb-1 line-clamp-1 transition-colors text-gray-200">
                                              {content.title}
                                            </h3>
                                            <p className="text-sm text-gray-300 line-clamp-2">
                                              {content.description}
                                            </p>
                                          </div>
                                          </button>
                                        );
                                      })}
                                    </div>
                                  </div>
                                </>
                              )}
                            </div>
                          ) : (
                            <div className="relative flex flex-col items-center justify-center h-full gap-4 z-10">
                              <div className="w-16 h-16 border-4 border-white/30 border-t-white rounded-full animate-spin" />
                              <p className="text-white text-xl font-semibold">
                                컨텐츠 선택 중입니다..
                              </p>
                            </div>
                          )
                        ) : (
                          <>
                            {/* 입장 인원 - 우측 하단 (컨텐츠 선택 단계에서만) */}
                            {!isGameStarting && !isRoleAssigned && !isPlaying && !isWaitingForRolePick && !isRoleSelectOpen && totalParticipants > 0 && (
                              <div className="absolute right-8 bottom-8 z-20 flex items-center gap-4 px-7 py-3 bg-white/10 backdrop-blur-sm rounded-full min-w-[140px] animate-slide-up">
                                <Users size={24} className="text-white flex-shrink-0" />
                                <div className="flex items-baseline gap-2">
                                  <span className="text-white text-2xl font-semibold w-4 text-center">{readyCount}</span>
                                  <span className="text-white/60 text-lg font-medium">/</span>
                                  <span className="text-white/80 text-xl font-medium w-4 text-center">{totalParticipants}</span>
                                </div>
                              </div>
                            )}

                            {/* 준비하기 + 시작하기 버튼 - 좌측 하단 (컨텐츠 선택 단계에서만) */}
                            {!isGameStarting && !isRoleAssigned && !isPlaying && !isWaitingForRolePick && !isRoleSelectOpen && (
                              <div className="absolute left-8 bottom-8 z-20 flex gap-4 animate-slide-up">
                                {/* 준비하기 버튼 */}
                                <button
                                  onClick={handleToggleReady}
                                  disabled={isReadyLoading}
                                  className={`w-40 py-3 rounded-full font-semibold text-lg transition-all flex items-center justify-center gap-2 text-white border-2 border-white/80 bg-transparent hover:bg-white/10 ${
                                    isReadyLoading
                                      ? "cursor-not-allowed opacity-60"
                                      : ""
                                  }`}
                                >
                                  {isReadyLoading ? (
                                    <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                  ) : isReady ? (
                                    <X size={18} />
                                  ) : (
                                    <Check size={18} />
                                  )}
                                  {isReadyLoading ? "처리 중..." : isReady ? "준비 취소" : "준비 완료"}
                                </button>

                                {/* 시작하기 버튼 (방장만, 모든 참가자 준비 완료 시) */}
                                {isOwner && allParticipantsReady && (
                                  <button
                                    onClick={handleStartShadowing}
                                    className="flex items-center gap-2 px-8 py-3 bg-white hover:bg-gray-100 text-gray-900 rounded-full font-semibold text-lg transition-all shadow-lg animate-fadeInUp"
                                  >
                                    <Play size={20} fill="currentColor" />
                                    시작하기
                                  </button>
                                )}
                              </div>
                            )}

                            {/* 역할 선택 완료 후 */}
                            {isRoleAssigned && (
                              <>
                                {/* 방장: 라운드 시작 버튼 - 좌측 하단 */}
                                {isOwner && (
                                  <div className="absolute left-8 bottom-8 z-20 flex flex-col gap-4">
                                    {/* 라운드 시작 버튼 (Round 2까지만) */}
                                    {!isFinishingRound && !isRoundInProgress && !isRoundStarting && currentRound < 2 && (
                                      <button
                                        onClick={() => handleStartRound(currentRound + 1)}
                                        disabled={isRoundStarting}
                                        className={`px-8 py-3 rounded-full font-semibold text-lg transition-all flex items-center gap-2 animate-fadeInUp ${
                                          isRoundStarting
                                            ? "bg-gray-400 cursor-not-allowed text-white"
                                            : "bg-white hover:bg-gray-100 text-gray-900"
                                        }`}
                                      >
                                        {isRoundStarting ? (
                                          <div className="w-5 h-5 border-2 border-gray-900 border-t-transparent rounded-full animate-spin" />
                                        ) : (
                                          <Play size={20} fill="currentColor" />
                                        )}
                                        {isRoundStarting ? "시작 중..." : `Round ${currentRound + 1} 시작하기`}
                                      </button>
                                    )}

                                    {/* Round 2 완료 후 버튼 */}
                                    {!isFinishingRound && !isRoundInProgress && currentRound === 2 && (
                                      <button
                                        onClick={handleFinishRoom}
                                        className="px-8 py-3 bg-green-600 hover:bg-green-700 text-white rounded-full font-semibold text-lg transition-all flex items-center gap-2"
                                      >
                                        <Home size={20} />
                                        처음으로 돌아가기
                                      </button>
                                    )}
                                  </div>
                                )}

                                {/* 방장이 아닌 사람: 대기 화면 */}
                                {!isOwner && !isRoundInProgress && currentRound < 2 && (
                                  <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-20 flex flex-col items-center gap-4">
                                    <div className="w-16 h-16 border-4 border-white/30 border-t-white rounded-full animate-spin" />
                                    <p className="text-white text-xl font-semibold">
                                      {isFinishingRound ? '라운드 종료 처리 중...' : '라운드 시작을 기다리는 중입니다'}
                                    </p>
                                  </div>
                                )}

                                {/* Round 2 완료 후 메시지 (방장이 아닌 사람) */}
                                {!isOwner && !isRoundInProgress && currentRound === 2 && (
                                  <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2 z-20">
                                    <p className="text-white text-xl font-semibold">
                                      모든 라운드가 완료되었습니다
                                    </p>
                                  </div>
                                )}
                              </>
                            )}
                          </>
                        )}
                      </>
                    )}
                  </>
                )}
              </>
            )}
            {/* WebRTC 비활성화 시 참여하기 버튼 숨김 */}
            {!DISABLE_WEBRTC && status === "idle" && (
              <button
                onClick={join}
                className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                참여하기
              </button>
            )}

            {/* 캐릭터 선택 버튼 - 역할 선택 완료 전까지만 표시 */}
            {(DISABLE_WEBRTC || status === "connected") && !isPlaying && !isGameStarting && countdown === null && !isRoleAssigned && availableRoles.length > 0 && (
              <button
                onClick={() => setIsRoleSelectOpen(true)}
                className="absolute top-4 left-4 flex items-center gap-2 px-4 py-2 bg-white/90 hover:bg-white text-gray-800 rounded-lg shadow-lg transition-colors font-medium"
              >
                <UserCircle size={20} />
                캐릭터 선택
              </button>
            )}
          </div>
        </div>

        <div className="border-t border-gray-200 p-4 bg-white">
          <VideoControls
            isAudioEnabled={isAudioEnabled}
            isVideoEnabled={isVideoEnabled}
            onToggleAudio={toggleAudio}
            onToggleVideo={toggleVideo}
            onLeave={handleLeave}
            volume={volume}
            onVolumeChange={setVolume}
            selectedAudioDevice={selectedAudioDevice}
            selectedVideoDevice={selectedVideoDevice}
            onAudioDeviceChange={handleAudioDeviceChange}
            onVideoDeviceChange={handleVideoDeviceChange}
            selectedNationality={selectedNationality}
            onNationalityChange={setSelectedNationality}
            isSubtitleEnabled={isSubtitleEnabled}
            onToggleSubtitle={toggleSubtitle}
          />
        </div>
      </div>

      {/* 오른쪽 사이드바 (동적 너비) */}
      <div className={`${layoutConfigs[layoutMode].width} flex flex-col border-l border-gray-200 bg-white transition-all duration-300`}>
        {/* 탭 버튼 */}
        <div className="flex gap-2 p-3 py-4 bg-gray-50">
          <div className="flex gap-2 flex-1">
            <button
              onClick={() => setSidebarTab("video")}
              className={`flex-1 flex items-center cursor-pointer justify-center gap-2 px-3 py-2 text-sm font-medium rounded-lg transition-all ${sidebarTab === "video"
                ? "bg-blue-600 text-white shadow-md"
                : "bg-white text-gray-600 hover:text-gray-900 hover:bg-gray-100 border border-gray-200"
                }`}
            >
              <Users size={18} />
              <span>참여자</span>
            </button>
            <button
              onClick={() => setSidebarTab("chat")}
              className={`flex-1 flex items-center cursor-pointer justify-center gap-2 px-3 py-2 text-sm font-medium rounded-lg transition-all ${sidebarTab === "chat"
                ? "bg-blue-600 text-white shadow-md"
                : "bg-white text-gray-600 hover:text-gray-900 hover:bg-gray-100 border border-gray-200"
                }`}
            >
              <MessageCircle size={18} />
              <span>채팅</span>
            </button>
          </div>

          {/* 레이아웃 드롭다운 */}
          <div className="relative" ref={layoutDropdownRef}>
            <button
              onClick={() => setIsLayoutDropdownOpen(!isLayoutDropdownOpen)}
              className="flex items-center justify-center gap-1 px-3 py-2 text-sm font-medium rounded-lg transition-all bg-white text-gray-600 hover:text-gray-900 hover:bg-gray-100 border border-gray-200"
              title="레이아웃 변경"
            >
              {(() => {
                const Icon = layoutConfigs[layoutMode].icon;
                return <Icon size={18} />;
              })()}
            </button>

            {/* 드롭다운 메뉴 */}
            {isLayoutDropdownOpen && (
              <div className="absolute top-full right-0 mt-2 bg-white border border-gray-200 rounded-lg shadow-lg overflow-hidden z-10 min-w-48">
                {(Object.entries(layoutConfigs) as [LayoutMode, typeof layoutConfigs[LayoutMode]][]).map(([mode, config]) => {
                  const Icon = config.icon;
                  return (
                    <button
                      key={mode}
                      onClick={() => handleLayoutChange(mode)}
                      className={`flex items-center gap-3 px-4 py-3 w-full hover:bg-gray-50 transition-colors text-left ${layoutMode === mode ? "bg-blue-50 text-blue-600" : "text-gray-700"
                        }`}
                    >
                      <Icon size={18} />
                      <span className="text-sm">{config.label}</span>
                    </button>
                  );
                })}
              </div>
            )}
          </div>
        </div>

        {/* 탭 콘텐츠 */}
        <div className="flex-1 overflow-hidden bg-white">
          {sidebarTab === "video" && isMediaChecked && (
            <div className={`h-full overflow-y-auto p-3 ${layoutMode === "grid" ? "grid grid-cols-2 gap-3 auto-rows-min" : "space-y-3"
              }`}>
              {/* WebRTC 비활성화 시 안내 메시지 표시 */}
              {DISABLE_WEBRTC ? (
                <div className="flex flex-col items-center justify-center h-full text-center px-4">
                  <p className="text-gray-600 text-lg font-semibold mb-2">WebRTC 비활성화됨</p>
                  <p className="text-gray-500 text-sm">쉐도잉 기능만 테스트 중입니다</p>
                  <p className="text-gray-400 text-xs mt-4">
                    WebRTC를 활성화하려면 DISABLE_WEBRTC를 false로 설정하세요
                  </p>
                </div>
              ) : status === "connected" && tiles.length > 0 ? (
                tiles.map((t) => {
                  // 각 타일의 준비 상태 확인
                  const tileIsReady = t.memberId !== undefined && participantsReady[t.memberId] === true;
                  // 게임 시작 중이거나 영상 재생 중이면 준비 상태 표시 안 함
                  const showReady = tileIsReady && !isGameStarting && !isPlaying;

                  // 방장 여부에 따라 라벨 수정
                  const isOwnerTile = t.memberId !== undefined && t.memberId === roomOwnerId;
                  const displayLabel = isOwnerTile ? `[방장] ${t.label}` : t.label;

                  // 디버깅: 타일 정보 로그
                  console.log('[VideoTile Debug]', {
                    label: t.label,
                    displayLabel,
                    memberId: t.memberId,
                    roomOwnerId,
                    isOwnerTile,
                    tileIsReady,
                    showReady,
                    participantsReady: participantsReady[t.memberId ?? -1],
                    isGameStarting,
                    isPlaying
                  });

                  return (
                    <VideoTile
                      key={t.id}
                      streamManager={t.streamManager}
                      muted={t.muted}
                      label={displayLabel}
                      isSpeaker={t.isSpeaker}
                      isReady={showReady}
                      isSettingUp={t.isSettingUp}
                      videoClassName={layoutMode === "wide" ? "aspect-[21/9]" : undefined}
                    />
                  );
                })
              ) : (
                <p className="text-center text-gray-500 text-sm py-8 col-span-2">
                  {status === "connecting" ? "연결 중..." : "참여자가 없습니다"}
                </p>
              )}
            </div>
          )}
          {sidebarTab === "chat" && (
            <div className="h-full">
              <ChatPanel
                messages={chatMessages}
                onSendMessage={sendChatMessage}
                nickname={nickname}
                currentUserId={memberId}
              />
            </div>
          )}
        </div>
      </div>

      {/* 비밀번호 입력 모달 */}
      {isPasswordModalOpen && (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
          <div className="bg-white rounded-lg p-6 w-96 shadow-xl">
            <h2 className="text-xl font-semibold mb-4 text-gray-900">비밀번호 입력</h2>
            <p className="text-sm text-gray-600 mb-4">이 방은 비밀번호로 보호되어 있습니다.</p>

            <input
              type="password"
              value={password}
              onChange={(e) => {
                setPassword(e.target.value);
                setPasswordError('');
              }}
              onKeyDown={(e) => {
                if (e.key === 'Enter') {
                  handlePasswordSubmit();
                }
              }}
              placeholder="비밀번호를 입력하세요"
              className="w-full px-4 py-2 border border-gray-300 rounded-lg focus:outline-none focus:ring-2 focus:ring-blue-500 mb-2"
              autoFocus
            />

            {passwordError && (
              <p className="text-sm text-red-600 mb-4">{passwordError}</p>
            )}

            <div className="flex gap-3 justify-end">
              <button
                onClick={() => {
                  setIsPasswordModalOpen(false);
                  setPassword('');
                  setPasswordError('');
                  navigate('/');
                }}
                className="px-4 py-2 text-gray-700 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
              >
                취소
              </button>
              <button
                onClick={handlePasswordSubmit}
                className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors"
              >
                입장하기
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 컨텐츠 선택 모달 */}
      {isContentSelectOpen && (
        <ContentSelectModal
          themeId={roomInfo.themeId}
          onClose={() => setIsContentSelectOpen(false)}
          onSelect={handleContentSelect}
        />
      )}

      {/* 역할 선택 모달 */}
      {isRoleSelectOpen && (
        <RoleSelectModal
          roles={availableRoles}
          onSelect={handleRoleSelect}
          onClose={() => setIsRoleSelectOpen(false)}
          onConfirm={handleConfirmRoles}
          isHost={isOwner}
          isConfirming={isConfirmingRoles}
          roomMembers={roomData?.members}
          selectedRoles={selectedRoles}
          currentUserId={memberId}
        />
      )}

      {/* 토스트 알림 */}
      {toastMessage && (
        <Toast
          message={toastMessage}
          type="error"
          onClose={() => setToastMessage(null)}
        />
      )}

      {/* 미디어 체크 모달 (블러 배경) */}
      {/* WebRTC 비활성화 시 미디어 체크 모달 표시 안 함 */}
      {!DISABLE_WEBRTC && !isMediaChecked && (
        <div className="fixed inset-0 z-[9999] backdrop-blur-sm bg-black/30">
          <MediaCheckScreen onJoin={handleMediaCheckComplete} roomTitle={roomInfo.title} />
        </div>
      )}
    </div>
  );
}
