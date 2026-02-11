import { useState, useEffect, useRef } from "react";
import { Mic, MicOff, Video, VideoOff, ChevronDown } from "lucide-react";

type MediaDeviceInfo = {
  deviceId: string;
  label: string;
};

type MediaCheckScreenProps = {
  onJoin: (audioEnabled: boolean, videoEnabled: boolean, audioDeviceId?: string, videoDeviceId?: string) => void;
  roomTitle: string;
};

export default function MediaCheckScreen({ onJoin, roomTitle }: MediaCheckScreenProps) {
  const [isAudioEnabled, setIsAudioEnabled] = useState(true);
  const [isVideoEnabled, setIsVideoEnabled] = useState(true);
  const [audioDevices, setAudioDevices] = useState<MediaDeviceInfo[]>([]);
  const [videoDevices, setVideoDevices] = useState<MediaDeviceInfo[]>([]);
  const [selectedAudioDevice, setSelectedAudioDevice] = useState<string>();
  const [selectedVideoDevice, setSelectedVideoDevice] = useState<string>();
  const [isAudioDeviceOpen, setIsAudioDeviceOpen] = useState(false);
  const [isVideoDeviceOpen, setIsVideoDeviceOpen] = useState(false);
  const [mediaStream, setMediaStream] = useState<MediaStream | null>(null);
  const [permissionError, setPermissionError] = useState<string | null>(null);
  const [isJoining, setIsJoining] = useState(false);

  const videoRef = useRef<HTMLVideoElement>(null);
  const audioDeviceRef = useRef<HTMLDivElement>(null);
  const videoDeviceRef = useRef<HTMLDivElement>(null);

  // 장치 목록 가져오기
  useEffect(() => {
    const getDevices = async () => {
      try {
        const devices = await navigator.mediaDevices.enumerateDevices();

        const audioInputs = devices
          .filter(device => device.kind === 'audioinput')
          .map(device => ({
            deviceId: device.deviceId,
            label: device.label || `마이크 ${device.deviceId.slice(0, 5)}`,
          }));

        const videoInputs = devices
          .filter(device => device.kind === 'videoinput')
          .map(device => ({
            deviceId: device.deviceId,
            label: device.label || `카메라 ${device.deviceId.slice(0, 5)}`,
          }));

        setAudioDevices(audioInputs);
        setVideoDevices(videoInputs);

        // 기본 장치 선택
        if (audioInputs.length > 0 && !selectedAudioDevice) {
          setSelectedAudioDevice(audioInputs[0].deviceId);
        }
        if (videoInputs.length > 0 && !selectedVideoDevice) {
          setSelectedVideoDevice(videoInputs[0].deviceId);
        }
      } catch (error) {
        console.error('Failed to enumerate devices:', error);
      }
    };

    getDevices();

    // 장치 변경 감지
    navigator.mediaDevices.addEventListener('devicechange', getDevices);
    return () => {
      navigator.mediaDevices.removeEventListener('devicechange', getDevices);
    };
  }, [selectedAudioDevice, selectedVideoDevice]);

  // 미디어 스트림 시작
  useEffect(() => {
    const startMediaStream = async () => {
      // 기존 스트림 정리
      if (mediaStream) {
        mediaStream.getTracks().forEach(track => track.stop());
        setMediaStream(null);
      }

      // 둘 다 꺼져있으면 미디어 스트림 요청하지 않음
      if (!isAudioEnabled && !isVideoEnabled) {
        setPermissionError(null);
        return;
      }

      try {
        setPermissionError(null);

        const constraints: MediaStreamConstraints = {
          audio: isAudioEnabled ? (selectedAudioDevice ? { deviceId: { exact: selectedAudioDevice } } : true) : false,
          video: isVideoEnabled ? (selectedVideoDevice ? { deviceId: { exact: selectedVideoDevice } } : true) : false,
        };

        const stream = await navigator.mediaDevices.getUserMedia(constraints);
        setMediaStream(stream);

        if (videoRef.current && isVideoEnabled) {
          videoRef.current.srcObject = stream;
        }
      } catch (error) {
        console.error('Failed to get media stream:', error);
        if (error instanceof Error) {
          if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
            setPermissionError('카메라 또는 마이크 접근 권한이 거부되었습니다. 브라우저 설정에서 권한을 허용해주세요.');
          } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
            setPermissionError('카메라 또는 마이크를 찾을 수 없습니다. 장치가 연결되어 있는지 확인해주세요.');
          } else {
            setPermissionError('미디어 장치에 접근할 수 없습니다.');
          }
        }
      }
    };

    startMediaStream();

    // 클린업
    return () => {
      if (mediaStream) {
        mediaStream.getTracks().forEach(track => track.stop());
      }
    };
  }, [isAudioEnabled, isVideoEnabled, selectedAudioDevice, selectedVideoDevice]);

  // 바깥 클릭 감지
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (audioDeviceRef.current && !audioDeviceRef.current.contains(event.target as Node)) {
        setIsAudioDeviceOpen(false);
      }
      if (videoDeviceRef.current && !videoDeviceRef.current.contains(event.target as Node)) {
        setIsVideoDeviceOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  const handleJoin = () => {
    setIsJoining(true);

    // 미디어 스트림 정리
    if (mediaStream) {
      mediaStream.getTracks().forEach(track => track.stop());
    }

    onJoin(isAudioEnabled, isVideoEnabled, selectedAudioDevice, selectedVideoDevice);
  };

  const handleAudioDeviceSelect = (deviceId: string) => {
    setSelectedAudioDevice(deviceId);
    setIsAudioDeviceOpen(false);
  };

  const handleVideoDeviceSelect = (deviceId: string) => {
    setSelectedVideoDevice(deviceId);
    setIsVideoDeviceOpen(false);
  };

  return (
    <div className="flex items-center justify-center min-h-screen p-6">
      <div className="bg-white/95 backdrop-blur-sm rounded-xl shadow-2xl w-full max-w-2xl p-6 border border-gray-200">
        <h1 className="text-2xl font-bold text-gray-900 mb-2">{roomTitle}</h1>
        <p className="text-gray-600 mb-6">입장하기 전에 카메라와 마이크를 확인해주세요</p>

        {permissionError && (
          <div className="mb-6 p-4 bg-red-50 border border-red-200 rounded-lg">
            <p className="text-red-800 text-sm">{permissionError}</p>
          </div>
        )}

        {/* 비디오 미리보기 */}
        <div className="mb-6">
          <div className="relative bg-linear-to-br from-gray-100 to-gray-200 rounded-lg overflow-hidden aspect-video">
            {isVideoEnabled ? (
              <video
                ref={videoRef}
                autoPlay
                playsInline
                muted
                className="w-full h-full object-cover mirror"
                style={{ transform: 'scaleX(-1)' }}
              />
            ) : (
              <div className="flex items-center justify-center h-full bg-linear-to-br from-blue-50 to-purple-50 backdrop-blur-sm">
                <div className="text-center">
                  <div className="inline-flex items-center justify-center w-20 h-20 bg-white/50 backdrop-blur-md rounded-full mb-4 border border-gray-200">
                    <VideoOff size={40} className="text-gray-400" />
                  </div>
                  <p className="text-gray-600">카메라가 꺼져있습니다</p>
                </div>
              </div>
            )}
          </div>
        </div>

        {/* 장치 선택 및 컨트롤 */}
        <div className="space-y-4 mb-6">
          {/* 오디오 장치 */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsAudioEnabled(!isAudioEnabled)}
              className={`flex items-center justify-center w-12 h-12 rounded-lg transition-colors ${
                isAudioEnabled
                  ? "bg-gray-100 hover:bg-gray-200 text-gray-800"
                  : "bg-red-50 hover:bg-red-100 text-red-600"
              }`}
              title={isAudioEnabled ? "음소거" : "음소거 해제"}
            >
              {isAudioEnabled ? <Mic size={24} /> : <MicOff size={24} />}
            </button>

            <div className="flex-1 relative" ref={audioDeviceRef}>
              <button
                onClick={() => setIsAudioDeviceOpen(!isAudioDeviceOpen)}
                disabled={!isAudioEnabled}
                className={`w-full flex items-center justify-between px-4 py-3 bg-gray-50 border border-gray-300 rounded-lg transition-colors ${
                  isAudioEnabled ? "hover:bg-gray-100" : "opacity-50 cursor-not-allowed"
                }`}
              >
                <span className="text-sm text-gray-700 truncate">
                  {audioDevices.find(d => d.deviceId === selectedAudioDevice)?.label || "마이크 선택"}
                </span>
                <ChevronDown size={20} className="text-gray-400 ml-2" />
              </button>

              {isAudioDeviceOpen && audioDevices.length > 0 && (
                <div className="absolute top-full mt-2 left-0 right-0 bg-white border border-gray-300 rounded-lg shadow-lg overflow-hidden z-10">
                  {audioDevices.map((device) => (
                    <button
                      key={device.deviceId}
                      onClick={() => handleAudioDeviceSelect(device.deviceId)}
                      className={`flex items-center gap-2 px-4 py-3 w-full hover:bg-gray-50 transition-colors text-left ${
                        selectedAudioDevice === device.deviceId ? "bg-blue-50 text-blue-600" : ""
                      }`}
                    >
                      <Mic size={16} />
                      <span className="text-sm truncate">{device.label}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>

          {/* 비디오 장치 */}
          <div className="flex items-center gap-3">
            <button
              onClick={() => setIsVideoEnabled(!isVideoEnabled)}
              className={`flex items-center justify-center w-12 h-12 rounded-lg transition-colors ${
                isVideoEnabled
                  ? "bg-gray-100 hover:bg-gray-200 text-gray-800"
                  : "bg-red-50 hover:bg-red-100 text-red-600"
              }`}
              title={isVideoEnabled ? "비디오 끄기" : "비디오 켜기"}
            >
              {isVideoEnabled ? <Video size={24} /> : <VideoOff size={24} />}
            </button>

            <div className="flex-1 relative" ref={videoDeviceRef}>
              <button
                onClick={() => setIsVideoDeviceOpen(!isVideoDeviceOpen)}
                disabled={!isVideoEnabled}
                className={`w-full flex items-center justify-between px-4 py-3 bg-gray-50 border border-gray-300 rounded-lg transition-colors ${
                  isVideoEnabled ? "hover:bg-gray-100" : "opacity-50 cursor-not-allowed"
                }`}
              >
                <span className="text-sm text-gray-700 truncate">
                  {videoDevices.find(d => d.deviceId === selectedVideoDevice)?.label || "카메라 선택"}
                </span>
                <ChevronDown size={20} className="text-gray-400 ml-2" />
              </button>

              {isVideoDeviceOpen && videoDevices.length > 0 && (
                <div className="absolute top-full mt-2 left-0 right-0 bg-white border border-gray-300 rounded-lg shadow-lg overflow-hidden z-10">
                  {videoDevices.map((device) => (
                    <button
                      key={device.deviceId}
                      onClick={() => handleVideoDeviceSelect(device.deviceId)}
                      className={`flex items-center gap-2 px-4 py-3 w-full hover:bg-gray-50 transition-colors text-left ${
                        selectedVideoDevice === device.deviceId ? "bg-blue-50 text-blue-600" : ""
                      }`}
                    >
                      <Video size={16} />
                      <span className="text-sm truncate">{device.label}</span>
                    </button>
                  ))}
                </div>
              )}
            </div>
          </div>
        </div>

        {/* 입장 버튼 */}
        <button
          onClick={handleJoin}
          disabled={isJoining}
          className="w-full py-4 bg-blue-600 text-white font-semibold rounded-lg hover:bg-blue-700 transition-colors disabled:bg-blue-400 disabled:cursor-not-allowed flex items-center justify-center gap-2"
        >
          {isJoining && (
            <div className="w-5 h-5 border-2 border-white border-t-transparent rounded-full animate-spin" />
          )}
          <span>{isJoining ? "입장 중..." : "입장하기"}</span>
        </button>
      </div>
    </div>
  );
}
