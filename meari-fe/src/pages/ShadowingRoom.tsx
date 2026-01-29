import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Users, MessageCircle, Lock, Unlock, Copy, Check, LayoutList, LayoutGrid, Maximize2, UserCircle } from "lucide-react";
// import Header from "../components/common/Header";
import VideoTile from "../components/webrtc/VideoTile";
import { useAuthStore } from "../store/auth.store";
import VideoControls from "../components/webrtc/VideoControls";
import ChatPanel from "../components/webrtc/ChatPanel";
import MediaCheckScreen from "../components/webrtc/MediaCheckScreen";
import ContentSelectModal from "../components/webrtc/ContentSelectModal";
import PasswordModal from "../components/webrtc/PasswordModal";
import Toast from "../components/common/Toast";
import RoleSelectModal from "../components/webrtc/RoleSelectModal";
import { useVideoRoom } from "../hooks/useVideoRoom";
import { useRoomWebSocket, type Role, type RoleSegment, type Sentence } from "../hooks/useRoomWebSocket";
import type { Content } from "../api/contents.api";
import { selectRoomContent, getContentRoles, type ContentRole } from "../api/contents.api";
import { getRoomDetail, enterRoom, leaveRoom, startGame, finishWatching, confirmRoles, startRound, finishRoom, getContentVideoUrl } from "../api/rooms.api";
import { useRoomStore } from "../store/room.store";
import { useRoleStore } from "../store/role.store";

type SidebarTab = "video" | "chat";
type LayoutMode = "narrow" | "grid" | "wide";

// TODO: 헤더 변경, 비디오 타일 변경
export default function ShadowingRoom() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const { userInfo } = useAuthStore();
  const roomOwnerId = useRoomStore((state) => state.roomData?.owner_id);
  const isOwner = userInfo?.member_id === roomOwnerId;
  const { roomData, setRoomData, setContentId, clearRoomData } = useRoomStore();
  const {
    availableRoles,
    mySelectedRoleId,
    setAvailableRoles,
    setMySelectedRole,
    setMemberRole,
    getConfirmData,
  } = useRoleStore();
  const [sidebarTab, setSidebarTab] = useState<SidebarTab>("video");
  const [isPasswordModalOpen, setIsPasswordModalOpen] = useState(false);
  const [passwordError, setPasswordError] = useState('');
  const [isRoomLoading, setIsRoomLoading] = useState(true);
  const [isEntered, setIsEntered] = useState(false);
  const [roomPassword, setRoomPassword] = useState<string | undefined>(undefined);
  const [copiedPassword, setCopiedPassword] = useState(false);
  const [layoutMode, setLayoutMode] = useState<LayoutMode>("narrow");
  const [isLayoutDropdownOpen, setIsLayoutDropdownOpen] = useState(false);
  const [isMediaChecked, setIsMediaChecked] = useState(false);
  const [isContentSelectOpen, setIsContentSelectOpen] = useState(false);
  const [selectedContent, setSelectedContent] = useState<Content | null>(null);
  const [videoUrl, setVideoUrl] = useState<string | null>(null);
  const [isHost] = useState(true); // Mock: 방장 여부 (실제로는 API나 WebSocket에서 설정)
  const [ isReady, setIsReady] = useState(false); // 내 준비 상태
  const [isRoleSelectOpen, setIsRoleSelectOpen] = useState(false); // 역할 선택 모달 상태
  const [isConfirmingRoles, setIsConfirmingRoles] = useState(false); // 역할 확정 로딩 상태
  const [isRoleAssigned, setIsRoleAssigned] = useState(false); // 역할 선택 완료 여부
  const [isGameStarting, setIsGameStarting] = useState(false); // 게임 시작 중 여부
  const [isRoundStarting, setIsRoundStarting] = useState(false); // 라운드 시작 중 여부
  const [currentRound, setCurrentRound] = useState(0); // 현재 라운드 (0: 시작 전)
  const [isRoundInProgress, setIsRoundInProgress] = useState(false); // 라운드 진행 중 여부
  const [isReadyLoading, setIsReadyLoading] = useState(false); // 준비 완료 로딩 상태
  const [participantsReady, setParticipantsReady] = useState<Record<number, boolean>>({});
  const [toastMessage, setToastMessage] = useState<string | null>(null);
  const layoutDropdownRef = useRef<HTMLDivElement>(null);
  const readyTimeoutRef = useRef<number | null>(null);
  const memberId = 1; // TODO: 실제 사용자 ID로 변경 필요

  // 영상 재생 관련 상태
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [countdown, setCountdown] = useState<number | null>(null);
  // Round용 자막 데이터 (여러 역할의 대본을 동시에 표시)
  interface SubtitleItem {
    roleName: string;
    roleId: number;
    text: string;
    isMyRole: boolean;
  }
  const [currentSubtitles, setCurrentSubtitles] = useState<SubtitleItem[]>([]);
  const [roleSegments, setRoleSegments] = useState<RoleSegment[]>([]);
  const [roundStartTime, setRoundStartTime] = useState<number | null>(null);
  const [timeUntilStart, setTimeUntilStart] = useState<number | null>(null);

  const nickname = userInfo?.nickname || "User";

  // 웹소켓 연결
  const {
    toggleReady: wsToggleReady,
    assignRole,
  } = useRoomWebSocket({
    roomId: Number(roomId),
    memberId,
    onReady: (message) => {
      // 준비 상태 메시지 수신
      if (message.member_id !== undefined && message.ready !== undefined) {
        setParticipantsReady(prev => ({
          ...prev,
          [message.member_id!]: message.ready!,
        }));

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
      console.log('Role pick received:', message);

      // store에서 최신 content_id 직접 가져오기 (클로저 문제 해결)
      const storeContentId = useRoomStore.getState().contentId;
      const currentContentId = storeContentId || message.content_id || selectedContent?.content_id;

      console.log('DEBUG - currentContentId:', currentContentId, 'from store:', storeContentId);

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

          console.log('Roles fetched from API:', roles);
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
      console.log('Role assigned:', message);
      // 역할 선점 성공 시 선택된 역할 ID 저장 (아직 확정은 아님)
      if (message.role_id && message.member_id) {
        setMySelectedRole(message.role_id);
        setMemberRole(message.member_id, message.role_id);
        setToastMessage('역할이 등록되었습니다');
      }
    },
    onRoleReleased: (message) => {
      console.log('Role released:', message);
      // 역할 해제 시 선택된 역할 ID 초기화
      setMySelectedRole(undefined);
      setToastMessage('역할이 해제되었습니다');
    },
    onGameStart: (message) => {
      console.log('Game starting:', message);
      // 게임 시작 시 즉시 영상 재생
      if (message.phase === 'WATCHING') {
        setIsPlaying(true);
      }
    },
    onPhaseWaiting: (message) => {
      console.log('Phase WAITING received:', message);
      // 대기 상태로 돌아가기
      resetToWaitingState();
    },
    onRolesConfirmed: (message) => {
      console.log('Roles confirmed:', message);
      // 대본 데이터 저장
      if (message.segments) {
        setRoleSegments(message.segments);
        setToastMessage('역할이 확정되었습니다');
      }
    },
    onRoundStart: (message) => {
      console.log('Round start received:', message);

      // 대본 데이터 저장
      if (message.segments) {
        console.log('Saving segments from ROUND_START:', message.segments);
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
        setRoundStartTime(message.server_time);
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
    onMemberJoin: (message) => {
      console.log('Member joined:', message);
      // TODO: 멤버 입장 시 처리 로직
    },
    onConnect: () => {
      console.log('WebSocket connected');
    },
    onDisconnect: () => {
      console.log('WebSocket disconnected');
    },
    onError: (error) => {
      console.error('WebSocket error:', error);
    },
  });

  // 방 정보 가져오기
  useEffect(() => {
    const fetchRoomDetail = async () => {
      if (!roomId) return;

      try {
        setIsRoomLoading(true);
        const response = await getRoomDetail(Number(roomId));

        if (!response.data.success || !response.data.data) {
          alert('방 정보를 가져올 수 없습니다.');
          navigate('/');
          return;
        }

        setRoomData(response.data.data);

        // 방장이면 enterRoom API 호출 없이 바로 입장
        if (isOwner) {
          setIsEntered(true);
        } else {
          // 비밀번호가 있는 방이면 비밀번호 모달 표시
          if (response.data.data.has_password) {
            setIsPasswordModalOpen(true);
          } else {
            // 비밀번호 없는 방은 enterRoom 호출
            try {
              const enterResponse = await enterRoom(Number(roomId), {});
              if (enterResponse.data.success) {
                setIsEntered(true);
              } else {
                alert('방 입장에 실패했습니다.');
                navigate('/');
              }
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
  }, [roomId, navigate, setRoomData]);

  // 방 퇴장 처리 (컴포넌트 언마운트 시)
  useEffect(() => {
    return () => {
      if (roomId && isEntered) {
        leaveRoom(Number(roomId)).catch((error) => {
          console.error('Failed to leave room:', error);
        });
        clearRoomData();
      }
    };
  }, [roomId, isEntered, clearRoomData]);

  // 브라우저 닫기/새로고침 시 퇴장 처리
  useEffect(() => {
    const handleBeforeUnload = () => {
      if (roomId && isEntered) {
        leaveRoom(Number(roomId)).catch((error) => {
          console.error('Failed to leave room on unload:', error);
        });
      }
    };

    window.addEventListener('beforeunload', handleBeforeUnload);
    return () => {
      window.removeEventListener('beforeunload', handleBeforeUnload);
    };
  }, [roomId, isEntered]);

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
      console.log('=== Round playback starting ===');
      console.log('Current round:', currentRound);

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

  // 영상 시간에 따른 자막 업데이트 (Round 모드: 모든 역할의 대본 표시)
  useEffect(() => {
    const video = videoRef.current;
    if (!video || roleSegments.length === 0) {
      console.log('Subtitle update skipped - video:', !!video, 'roleSegments length:', roleSegments.length);
      return;
    }

    console.log('Setting up subtitle update with roleSegments:', roleSegments);

    const updateSubtitle = () => {
      const currentTime = video.currentTime;
      const subtitles: SubtitleItem[] = [];

      // 모든 세그먼트의 문장들을 순회하며 현재 시간에 맞는 모든 문장 찾기
      for (const segment of roleSegments) {
        const sentence = segment.sentences.find(
          (s: Sentence) => currentTime >= s.start_time && currentTime <= s.end_time
        );

        if (sentence) {
          subtitles.push({
            roleName: segment.role_name,
            roleId: segment.role_id,
            text: sentence.text_ko, // TODO: selectedNationality에 따라 text_vn 선택
            isMyRole: segment.role_id === mySelectedRoleId,
          });
        }
      }

      if (subtitles.length > 0) {
        console.log(`Subtitles at ${currentTime}s:`, subtitles);
      }
      setCurrentSubtitles(subtitles);
    };

    video.addEventListener('timeupdate', updateSubtitle);
    return () => video.removeEventListener('timeupdate', updateSubtitle);
  }, [roleSegments, mySelectedRoleId, isPlaying]);

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

  // 비밀번호 검증 처리
  const handlePasswordSubmit = async (password: string) => {
    if (!roomId) return;

    // eslint-disable-next-line no-useless-catch
    try {
      const response = await enterRoom(Number(roomId), { password });

      if (response.data.success) {
        setRoomPassword(password);
        setIsPasswordModalOpen(false);
        setIsEntered(true);
        setPasswordError('');
      } else {
        setPasswordError(response.data.error?.message || '비밀번호가 일치하지 않습니다.');
        throw new Error('Invalid password');
      }
    } catch (error) {
      throw error;
    }
  };

  const handlePasswordCancel = () => {
    navigate('/');
  };

  // 방 정보 (store에서 가져오기)
  const roomInfo = roomData ? {
    isLocked: roomData.has_password,
    title: roomData.title,
    password: '',
    themeId: roomData.theme_id
  } : {
    isLocked: false,
    title: "Loading...",
    password: "",
    themeId: 1
  };

  const handleContentSelect = async (content: Content) => {
    if (!roomId) return;

    try {
      // 방 컨텐츠 선택 API 호출 (실제 content_id 전달)
      const response = await selectRoomContent(Number(roomId), content.content_id);

      if (response.data.success) {
        // content_id를 store에 저장
        setContentId(content.content_id);

        // 비디오 URL 가져오기
        try {
          const videoResponse = await getContentVideoUrl(content.content_id);
          if (videoResponse.data.success && videoResponse.data.data) {
            setVideoUrl(videoResponse.data.data.video_url);
          }
        } catch (error) {
          console.error('Failed to get video URL:', error);
        }

        setSelectedContent(content);
        setIsContentSelectOpen(false);
        // 컨텐츠 선택 시 모든 참가자의 준비 상태 초기화
        setIsReady(false);
        setParticipantsReady({});
      } else {
        setToastMessage('컨텐츠 선택에 실패했어요');
      }
    } catch (error) {
      setToastMessage('컨텐츠 선택에 실패했어요');
    }
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
    if (!roomId) return;

    try {
      setIsGameStarting(true); // 게임 시작 중 상태로 변경
      // 방장이 게임 시작 API 호출
      const response = await startGame(Number(roomId));

      if (response.data.success) {
        console.log('Game start API called successfully');
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
    setIsReady(false);
    setSelectedContent(null);
    setVideoUrl(null);
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
    isAudioEnabled,
    isVideoEnabled,
    join,
    leave,
    toggleAudio,
    toggleVideo,
  } = useVideoRoom({
    roomId: Number(roomId),
    nickname,
    password: roomPassword,
    autoJoin: false,
    isOwner
  });

  const [volume, setVolume] = useState(100);
  const [selectedAudioDevice, setSelectedAudioDevice] = useState<string>();
  const [selectedVideoDevice, setSelectedVideoDevice] = useState<string>();
  const [selectedNationality, setSelectedNationality] = useState<"KR" | "VN">("KR");
  const [isSubtitleEnabled, setIsSubtitleEnabled] = useState(false);

  const toggleSubtitle = () => setIsSubtitleEnabled(!isSubtitleEnabled);

  // 모든 참가자가 준비 완료되었는지 확인
  const totalParticipants = tiles.length;
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

  // 미디어 체크 완료 후 WebRTC 연결
  useEffect(() => {
    if (isMediaChecked && isEntered && roomId && status === 'idle') {
      join();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [isMediaChecked, isEntered, roomId, status]);

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

  const handleLeave = async () => {
    if (roomId) {
      try {
        await leave();
        await leaveRoom(Number(roomId));
        clearRoomData();
      } catch (error) {
        console.error('Failed to leave room:', error);
      }
    }
    navigate("/");
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
    _audioEnabled: boolean,
    _videoEnabled: boolean,
    audioDeviceId?: string,
    videoDeviceId?: string
  ) => {
    setIsMediaChecked(true);

    // 선택된 장치 정보 저장
    setSelectedAudioDevice(audioDeviceId);
    setSelectedVideoDevice(videoDeviceId);
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

  // 비밀번호 입력 모달
  if (isPasswordModalOpen) {
    return (
      <PasswordModal
        roomTitle={roomInfo.title}
        onSubmit={handlePasswordSubmit}
        onCancel={handlePasswordCancel}
        errorMessage={passwordError}
      />
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

  // 미디어 체크가 완료되지 않았으면 미디어 체크 화면 표시
  if (!isMediaChecked) {
    return <MediaCheckScreen onJoin={handleMediaCheckComplete} roomTitle={roomInfo.title} />;
  }

  return (
    <div className="flex h-screen bg-gray-50">
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
          <div className="relative h-full w-full rounded-lg bg-gray-900 flex items-center justify-center">
            {status === "connecting" && (
              <div className="flex flex-col items-center gap-3">
                <div className="w-10 h-10 border-4 border-blue-600 border-t-transparent rounded-full animate-spin" />
                <p className="text-gray-400">연결 중...</p>
              </div>
            )}
            {status === "error" && (
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
            {status === "connected" && (
              <>
                {/* 카운트다운 오버레이 */}
                {countdown !== null && (
                  <div className="absolute inset-0 flex items-center justify-center bg-black/70 z-20 rounded-lg">
                    <div className="text-white text-9xl font-bold animate-pulse">
                      {countdown}
                    </div>
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
                      onEnded={async () => {
                        // 영상 재생 완료 시 처리
                        setIsPlaying(false);

                        // Round 진행 중이면 라운드 종료
                        if (currentRound >= 1 && isRoundInProgress) {
                          setIsRoundInProgress(false);
                          setToastMessage(`Round ${currentRound} 완료`);
                        } else if (roomId) {
                          // 첫 번째 시청 완료 시 finishWatching API 호출
                          try {
                            const response = await finishWatching(Number(roomId));
                            if (response.data.success) {
                              console.log('Watching finished successfully');
                              setIsGameStarting(false);
                            } else {
                              console.error('Failed to finish watching:', response.data.error?.message);
                            }
                          } catch (error) {
                            console.error('Failed to finish watching:', error);
                          }
                        }
                      }}
                    />

                    {/* 대본 표시 (Round 1 진행 중) */}
                    {currentRound >= 1 && currentSubtitles.length > 0 && (
                      <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 w-full max-w-4xl px-4">
                        <div className="bg-black/90 px-6 py-4 rounded-xl space-y-3">
                          {currentSubtitles.map((subtitle, index) => (
                            <div
                              key={`${subtitle.roleId}-${index}`}
                              className={`flex flex-col gap-1 p-3 rounded-lg transition-all ${
                                subtitle.isMyRole
                                  ? 'bg-blue-600/40 border-2 border-blue-400'
                                  : 'bg-white/10'
                              }`}
                            >
                              <p
                                className={`text-sm font-semibold ${
                                  subtitle.isMyRole ? 'text-blue-200' : 'text-gray-300'
                                }`}
                              >
                                {subtitle.roleName}
                                {subtitle.isMyRole && ' (내 역할)'}
                              </p>
                              <p
                                className={`text-lg font-medium ${
                                  subtitle.isMyRole ? 'text-white' : 'text-gray-200'
                                }`}
                              >
                                {subtitle.text}
                              </p>
                            </div>
                          ))}
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
                      <div className="absolute inset-0">
                        <video
                          src={videoUrl}
                          className="w-full h-full object-cover"
                          style={{ filter: 'blur(20px)', transform: 'scale(1.1)' }}
                          muted
                          playsInline
                        />
                        <div className="absolute inset-0 bg-black/40" />
                      </div>
                    )}

                    <div className="relative flex flex-col items-center gap-4 z-10">
                      {!selectedContent ? (
                        <div className="text-center">
                          <p className="text-gray-500 text-sm mb-2">쉐도잉 콘텐츠 영역</p>
                          {isHost && (
                            <p className="text-gray-400 text-xs">컨텐츠를 선택해주세요</p>
                          )}
                        </div>
                      ) : (
                        <div className="text-center">
                          <p className="text-gray-200 font-medium mb-2">현재 컨텐츠</p>
                          <p className="text-white text-lg font-semibold mb-4">{selectedContent.title}</p>

                        <div className="flex flex-col items-center gap-3">
                          {/* 게임 시작 전: 준비 완료 및 시작 버튼 */}
                          {!isGameStarting && !isRoleAssigned && (
                            <>
                              {/* 준비 완료 버튼 (모든 참가자) */}
                              <button
                                onClick={handleToggleReady}
                                disabled={isReadyLoading}
                                className={`px-6 py-3 rounded-lg font-semibold transition-all flex items-center gap-2 ${
                                  isReadyLoading
                                    ? "bg-gray-400 cursor-not-allowed text-white"
                                    : isReady
                                    ? "bg-green-500 hover:bg-green-600 text-white"
                                    : "bg-blue-600 hover:bg-blue-700 text-white"
                                }`}
                              >
                                {isReadyLoading && (
                                  <div className="w-4 h-4 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                )}
                                {isReadyLoading ? "처리 중..." : isReady ? "준비 완료" : "준비하기"}
                              </button>

                              {/* 시작 버튼 (방장만) */}
                              {isHost && (
                                <div className="flex flex-col items-center gap-2 mt-2">
                                  {totalParticipants > 0 && (
                                    <div className="text-sm text-gray-600 mb-1">
                                      준비 완료: {readyCount} / {totalParticipants}
                                    </div>
                                  )}
                                  <button
                                    onClick={handleStartShadowing}
                                    disabled={!allParticipantsReady}
                                    className={`px-8 py-3 rounded-lg font-semibold transition-all ${
                                      allParticipantsReady
                                        ? "bg-blue-600 hover:bg-blue-700 text-white"
                                        : "bg-gray-300 text-gray-500 cursor-not-allowed"
                                    }`}
                                  >
                                    쉐도잉 시작
                                  </button>
                                  {!allParticipantsReady && (
                                    <p className="text-xs text-gray-500">모든 참가자가 준비될 때까지 기다려주세요</p>
                                  )}
                                </div>
                              )}
                            </>
                          )}

                          {/* 역할 선택 완료 후: Round 버튼 (방장만) */}
                          {isRoleAssigned && isHost && (
                            <>
                              {/* 라운드 시작 버튼 (Round 2까지만) */}
                              {!isRoundInProgress && !isRoundStarting && currentRound < 2 && (
                                <button
                                  onClick={() => handleStartRound(currentRound + 1)}
                                  disabled={isRoundStarting}
                                  className={`px-8 py-3 rounded-lg font-semibold transition-all flex items-center gap-2 ${
                                    isRoundStarting
                                      ? "bg-gray-400 cursor-not-allowed text-white"
                                      : "bg-blue-600 hover:bg-blue-700 text-white"
                                  }`}
                                >
                                  {isRoundStarting && (
                                    <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
                                  )}
                                  {isRoundStarting ? "시작 중..." : `Round ${currentRound + 1} 시작하기`}
                                </button>
                              )}

                              {/* Round 2 완료 후 메시지 및 버튼 */}
                              {!isRoundInProgress && currentRound === 2 && (
                                <div className="flex flex-col items-center gap-4">
                                  <p className="text-gray-700 text-lg font-semibold">
                                    모든 라운드가 완료되었습니다
                                  </p>
                                  <button
                                    onClick={handleFinishRoom}
                                    className="px-8 py-3 bg-green-600 hover:bg-green-700 text-white rounded-lg font-semibold transition-all"
                                  >
                                    처음으로 돌아가기
                                  </button>
                                </div>
                              )}
                            </>
                          )}
                        </div>
                        </div>
                      )}
                    </div>
                  </>
                )}
              </>
            )}
            {status === "idle" && (
              <button
                onClick={join}
                className="px-6 py-3 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
              >
                참여하기
              </button>
            )}

            {/* 컨텐츠 변경 버튼 (방장만) - 영상 재생 중이 아니고 게임 시작 전일 때만 표시 */}
            {status === "connected" && isHost && !isPlaying && !isGameStarting && countdown === null && (
              <button
                onClick={() => setIsContentSelectOpen(true)}
                className="absolute top-4 right-4 px-4 py-2 bg-white/90 hover:bg-white text-gray-800 rounded-lg shadow-lg transition-colors font-medium"
              >
                컨텐츠 변경
              </button>
            )}

            {/* 캐릭터 선택 버튼 - 역할 선택 완료 전까지만 표시 */}
            {status === "connected" && !isPlaying && !isGameStarting && countdown === null && !isRoleAssigned && availableRoles.length > 0 && (
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
          {sidebarTab === "video" && (
            <div className={`h-full overflow-y-auto p-3 ${layoutMode === "grid" ? "grid grid-cols-2 gap-3 auto-rows-min" : "space-y-3"
              }`}>
              {status === "connected" && tiles.length > 0 ? (
                tiles.map((t) => (
                  <VideoTile
                    key={t.id}
                    streamManager={t.streamManager}
                    muted={t.muted}
                    label={t.label}
                    isSpeaker={t.isSpeaker}
                    isReady={t.id === "me" ? (isReady && !isGameStarting && !isPlaying) : false}
                    videoClassName={layoutMode === "wide" ? "aspect-[21/9]" : undefined}
                  />
                ))
              ) : (
                <p className="text-center text-gray-500 text-sm py-8 col-span-2">
                  {status === "connecting" ? "연결 중..." : "참여자가 없습니다"}
                </p>
              )}
            </div>
          )}
          {sidebarTab === "chat" && (
            <div className="h-full">
              <ChatPanel roomId={roomId} />
            </div>
          )}
        </div>
      </div>

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
          selectedRoleId={mySelectedRoleId}
          isHost={isHost}
          isConfirming={isConfirmingRoles}
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
    </div>
  );
}
