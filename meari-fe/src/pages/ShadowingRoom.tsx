import { useState, useEffect, useRef } from "react";
import { useParams, useNavigate } from "react-router-dom";
import { Users, MessageCircle, Lock, Unlock, Copy, Check, LayoutList, LayoutGrid, Maximize2 } from "lucide-react";
// import Header from "../components/common/Header";
import VideoTile from "../components/webrtc/VideoTile";
import { useAuthStore } from "../store/auth.store";
import VideoControls from "../components/webrtc/VideoControls";
import ChatPanel from "../components/webrtc/ChatPanel";
import MediaCheckScreen from "../components/webrtc/MediaCheckScreen";
import ContentSelectModal from "../components/webrtc/ContentSelectModal";
import PasswordModal from "../components/webrtc/PasswordModal";
import { useVideoRoom } from "../hooks/useVideoRoom";
import type { Content } from "../api/contents.api";
import { getRoomDetail, enterRoom, leaveRoom } from "../api/rooms.api";
import { useRoomStore } from "../store/room.store";

type SidebarTab = "video" | "chat";
type LayoutMode = "narrow" | "grid" | "wide";

// TODO: 헤더 변경, 비디오 타일 변경
export default function ShadowingRoom() {
  const { roomId } = useParams<{ roomId: string }>();
  const navigate = useNavigate();
  const { userInfo } = useAuthStore();
  const roomOwnerId = useRoomStore((state) => state.roomData?.owner_id);
  const isOwner = userInfo?.member_id === roomOwnerId;
  const { roomData, setRoomData, clearRoomData } = useRoomStore();
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
  const [, setInitialAudioEnabled] = useState(true);
  const [, setInitialVideoEnabled] = useState(true);
  const [, setInitialAudioDeviceId] = useState<string>();
  const [, setInitialVideoDeviceId] = useState<string>();
  const [isContentSelectOpen, setIsContentSelectOpen] = useState(false);
  const [selectedContent, setSelectedContent] = useState<Content | null>(null);
  const [isHost,] = useState(true); // Mock: 방장 여부 (실제로는 API나 WebSocket에서 설정)
  const [isReady, setIsReady] = useState(false); // 내 준비 상태
  const [participantsReady, setParticipantsReady] = useState<Record<string, boolean>>({
    me: false,
    user1: false,
    user2: false,
    user3: false,
  }); // 각 참가자의 준비 상태
  const layoutDropdownRef = useRef<HTMLDivElement>(null);

  // 영상 재생 관련 상태
  const videoRef = useRef<HTMLVideoElement>(null);
  const [isPlaying, setIsPlaying] = useState(false);
  const [countdown, setCountdown] = useState<number | null>(null);
  const [currentSubtitle, setCurrentSubtitle] = useState<string>("");
  const [subtitles, setSubtitles] = useState<Array<{ start: number; end: number; text: string }>>([]);

  const nickname = userInfo?.nickname || "User";

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

  // 자막 데이터 로드
  useEffect(() => {
    const loadSubtitles = async () => {
      if (selectedContent) {
        try {
          const useMock = import.meta.env.VITE_USE_MOCK_CONTENTS === 'true';
          if (useMock) {
            // Mock 자막 데이터 로드
            const response = await fetch('/src/assets/video/description.txt');
            const data = await response.json();
            setSubtitles(data);
          }
        } catch (error) {
          console.error('Failed to load subtitles:', error);
        }
      }
    };
    loadSubtitles();
  }, [selectedContent]);

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
  }, [countdown]);

  // 영상 시간에 따른 자막 업데이트
  useEffect(() => {
    const video = videoRef.current;
    if (!video) return;

    const updateSubtitle = () => {
      const currentTime = video.currentTime;
      const subtitle = subtitles.find(
        (sub) => currentTime >= sub.start && currentTime <= sub.end
      );
      setCurrentSubtitle(subtitle ? subtitle.text : "");
    };

    video.addEventListener('timeupdate', updateSubtitle);
    return () => video.removeEventListener('timeupdate', updateSubtitle);
  }, [subtitles]);

  // 모든 참가자가 준비 완료되었는지 확인
  // TODO: 실제 배포 시에는 Object.values(participantsReady).every(ready => ready)로 변경
  const allParticipantsReady = participantsReady.me; // 테스트: 본인만 준비되면 시작 가능

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

  const handleContentSelect = (content: Content) => {
    setSelectedContent(content);
    setIsContentSelectOpen(false);
    // 컨텐츠 선택 시 모든 참가자의 준비 상태 초기화
    setIsReady(false);
    setParticipantsReady({
      me: false,
      user1: false,
      user2: false,
      user3: false,
    });
    // TODO: 실제로는 선택한 컨텐츠의 비디오를 로드하고 재생
    console.log('Selected content:', content);
  };

  const handleToggleReady = () => {
    const newReadyState = !isReady;
    setIsReady(newReadyState);
    setParticipantsReady(prev => ({
      ...prev,
      me: newReadyState,
    }));
    // TODO: WebSocket으로 준비 상태 전송
    console.log('Ready state:', newReadyState);
  };

  const handleStartShadowing = () => {
    // 카운트다운 시작 (3초)
    setCountdown(3);
    console.log('Starting shadowing countdown...');
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
    audioEnabled: boolean,
    videoEnabled: boolean,
    audioDeviceId?: string,
    videoDeviceId?: string
  ) => {
    setInitialAudioEnabled(audioEnabled);
    setInitialVideoEnabled(videoEnabled);
    setInitialAudioDeviceId(audioDeviceId);
    setInitialVideoDeviceId(videoDeviceId);
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

                {/* 영상 재생 중 */}
                {isPlaying && selectedContent && (
                  <div className="relative w-full h-full">
                    <video
                      ref={videoRef}
                      src={selectedContent.video_url}
                      className="w-full h-full object-contain"
                      onContextMenu={(e) => e.preventDefault()}
                      style={{ pointerEvents: 'none' }}
                    />

                    {/* 자막 표시 */}
                    {isSubtitleEnabled && currentSubtitle && (
                      <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 bg-black/80 px-6 py-3 rounded-lg">
                        <p className="text-white text-xl font-medium text-center whitespace-pre-line">
                          {currentSubtitle}
                        </p>
                      </div>
                    )}
                  </div>
                )}

                {/* 영상 재생 전 */}
                {!isPlaying && (
                  <div className="flex flex-col items-center gap-4">
                    {!selectedContent ? (
                      <div className="text-center">
                        <p className="text-gray-500 text-sm mb-2">쉐도잉 콘텐츠 영역</p>
                        {isHost && (
                          <p className="text-gray-400 text-xs">컨텐츠를 선택해주세요</p>
                        )}
                      </div>
                    ) : (
                      <div className="text-center">
                        <p className="text-gray-600 font-medium mb-2">현재 컨텐츠</p>
                        <p className="text-gray-900 text-lg font-semibold mb-4">{selectedContent.title}</p>

                        <div className="flex flex-col items-center gap-3">
                          {/* 준비 완료 버튼 (모든 참가자) */}
                          <button
                            onClick={handleToggleReady}
                            className={`px-6 py-3 rounded-lg font-semibold transition-all ${isReady
                              ? "bg-green-500 hover:bg-green-600 text-white"
                              : "bg-blue-600 hover:bg-blue-700 text-white"
                              }`}
                          >
                            {isReady ? "준비 완료" : "준비하기"}
                          </button>

                          {/* 시작 버튼 (방장만) */}
                          {isHost && (
                            <div className="flex flex-col items-center gap-2 mt-2">
                              <div className="text-sm text-gray-600 mb-1">
                                준비 완료: {Object.values(participantsReady).filter(r => r).length} / {Object.keys(participantsReady).length}
                              </div>
                              <button
                                onClick={handleStartShadowing}
                                disabled={!allParticipantsReady}
                                className={`px-8 py-3 rounded-lg font-semibold transition-all ${allParticipantsReady
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
                        </div>
                      </div>
                    )}
                  </div>
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

            {/* 컨텐츠 변경 버튼 (방장만) - 영상 재생 중이 아닐 때만 표시 */}
            {status === "connected" && isHost && !isPlaying && (
              <button
                onClick={() => setIsContentSelectOpen(true)}
                className="absolute top-4 right-4 px-4 py-2 bg-white/90 hover:bg-white text-gray-800 rounded-lg shadow-lg transition-colors font-medium"
              >
                컨텐츠 변경
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
                    isReady={t.isReady}
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
    </div>
  );
}
