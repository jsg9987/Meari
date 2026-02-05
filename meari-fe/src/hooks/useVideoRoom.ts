import { useEffect, useMemo, useRef, useState, useCallback } from "react";
import { OpenVidu, Publisher, Session, Subscriber, Connection } from "openvidu-browser";
import { enterWebRTC } from "../api/rooms.api";
import { createSession, createConnection, deleteSession } from "../api/webrtc.api";
import { useAuthStore } from "../store/auth.store";

export type ConnectionStatus = "idle" | "connecting" | "connected" | "error";

export interface VideoTileData {
  id: string;
  streamManager?: Publisher | Subscriber;
  muted?: boolean;
  label?: string;
  isSpeaker?: boolean;
  isReady?: boolean;
  isSettingUp?: boolean; // 세팅 중 (아직 publish 안 함)
  memberId?: number; // 멤버 ID
}

interface UseVideoRoomOptions {
  roomId: number;
  nickname: string;
  password?: string;
  autoJoin?: boolean;
  autoPublish?: boolean; // 자동으로 publish 할지 여부
  isOwner?: boolean;
  memberId?: number;
  roleId?: number | null;
}

export function useVideoRoom({
  roomId,
  nickname,
  password,
  autoJoin = false,
  autoPublish = true,
  isOwner = false,
  roleId = null
}: UseVideoRoomOptions) {
  const { userInfo } = useAuthStore();
  const memberId = userInfo?.memberId;
  const [session, setSession] = useState<Session | null>(null);
  const [publisher, setPublisher] = useState<Publisher | null>(null);
  const [subscribers, setSubscribers] = useState<Subscriber[]>([]);
  const [connections, setConnections] = useState<Connection[]>([]); // 아직 publish 안 한 연결들
  const [status, setStatus] = useState<ConnectionStatus>("idle");
  const [error, setError] = useState<string | null>(null);
  const [isAudioEnabled, setIsAudioEnabled] = useState(true);
  const [isVideoEnabled, setIsVideoEnabled] = useState(true);
  const [backendSessionId, setBackendSessionId] = useState<string | null>(null); // 백엔드에서 받은 session_id

  const ovRef = useRef<OpenVidu | null>(null);
  const statusRef = useRef<ConnectionStatus>("idle");
  const publisherRef = useRef<Publisher | null>(null);

  const tiles = useMemo<VideoTileData[]>(() => {
    const arr: VideoTileData[] = [];
    if (publisher) {
      arr.push({ id: "me", streamManager: publisher, muted: true, label: `${nickname} (나)`, memberId });
    }

    // 실제 스트림이 있는 참가자들
    subscribers.forEach((s) => {
      const clientData = s.stream.connection.data;
      let name = "참여자";
      let memberIdFromData: number | undefined;
      try {
        // %/% 구분자로 나눠진 경우 처리 (백엔드에서 추가 데이터를 넣은 경우)
        if (clientData.includes('%/%')) {
          const parts = clientData.split('%/%');
          // 두 번째 부분(백엔드 데이터)에서 nickname과 member_id 추출
          const backendData = JSON.parse(parts[1]);
          name = backendData.nickname || name;
          memberIdFromData = backendData.member_id;
        } else {
          const parsed = JSON.parse(clientData);
          name = parsed.clientData || parsed.nickname || name;
          memberIdFromData = parsed.member_id;
        }
      } catch (error) {
        console.error('Failed to parse clientData:', clientData, error);
        name = "참여자";
      }
      arr.push({ id: s.stream.streamId, streamManager: s, label: name, memberId: memberIdFromData });
    });

    // 아직 publish 안 한 참가자들 (세팅 중)
    connections.forEach((conn) => {
      const clientData = conn.data;
      let name = "참여자";
      let memberIdFromData: number | undefined;
      try {
        if (clientData.includes('%/%')) {
          const parts = clientData.split('%/%');
          const backendData = JSON.parse(parts[1]);
          name = backendData.nickname || name;
          memberIdFromData = backendData.member_id;
        } else {
          const parsed = JSON.parse(clientData);
          name = parsed.clientData || parsed.nickname || name;
          memberIdFromData = parsed.member_id;
        }
      } catch (error) {
        console.error('Failed to parse clientData:', clientData, error);
        name = "참여자";
      }
      arr.push({ id: conn.connectionId, label: name, isSettingUp: true, memberId: memberIdFromData });
    });

    return arr;
  }, [publisher, subscribers, connections, nickname, memberId]);

  const join = useCallback(async () => {
    if (statusRef.current === "connecting" || statusRef.current === "connected") {
      console.log("Already connecting or connected, skipping join");
      return;
    }

    statusRef.current = "connecting";
    setStatus("connecting");
    setError(null);

    const OV = new OpenVidu();
    ovRef.current = OV;
    const mySession = OV.initSession();

    mySession.on("connectionCreated", (event) => {
      // 나 자신이 아니고, 아직 스트림이 없는 연결 (세팅 중)
      if (event.connection.connectionId !== mySession.connection?.connectionId) {
        setConnections((prev) => [...prev, event.connection]);
      }
    });

    mySession.on("connectionDestroyed", (event) => {
      setConnections((prev) =>
        prev.filter((c) => c.connectionId !== event.connection.connectionId)
      );
    });

    mySession.on("streamCreated", (event) => {
      const subscriber = mySession.subscribe(event.stream, undefined);
      setSubscribers((prev) => [...prev, subscriber]);

      // 스트림이 생성되면 connections에서 제거 (더 이상 세팅 중이 아님)
      setConnections((prev) =>
        prev.filter((c) => c.connectionId !== event.stream.connection.connectionId)
      );
    });

    mySession.on("streamDestroyed", (event) => {
      const streamId = event.stream.streamId;
      setSubscribers((prev) => prev.filter((s) => s.stream.streamId !== streamId));
    });

    mySession.on("exception", (ex) => {
      console.warn("OpenVidu exception", ex);
    });

    try {
      let token: string;

      if (isOwner) {
        // 방장: 세션 생성 -> 연결 토큰 생성
        const sessionResponse = await createSession({
          custom_session_id: `room_${roomId}`,
          room_id: roomId
        });

        if (!sessionResponse.data.success || !sessionResponse.data.data) {
          throw new Error(sessionResponse.data.error?.message || "세션 생성에 실패했습니다");
        }

        const { session_id } = sessionResponse.data.data;

        const connectionResponse = await createConnection(session_id, {
          member_id: memberId as number,
          nickname,
          role_id: roleId
        });

        if (!connectionResponse.data.success || !connectionResponse.data.data) {
          throw new Error(connectionResponse.data.error?.message || "연결 토큰 생성에 실패했습니다");
        }

        // 백엔드에서 받은 session_id 저장
        setBackendSessionId(session_id);
        // 백엔드에서 받은 토큰을 그대로 사용
        token = connectionResponse.data.data.token;
      } else {
        // 일반 사용자: enterWebRTC 사용
        const webrtcResponse = await enterWebRTC(roomId, { password });

        if (!webrtcResponse.data.success || !webrtcResponse.data.data) {
          throw new Error(webrtcResponse.data.error?.message || "WebRTC 입장에 실패했습니다");
        }

        // 백엔드에서 받은 session_id 저장
        setBackendSessionId(webrtcResponse.data.data.sessionId);
        // 백엔드에서 받은 토큰을 그대로 사용
        token = webrtcResponse.data.data.token;
      }

      await mySession.connect(token, { clientData: nickname });

      // 이미 세션에 있는 connections를 수동으로 추가 (늦게 들어온 경우 대비)
      const existingConnections = mySession.remoteConnections;
      if (existingConnections) {
        Object.values(existingConnections).forEach((conn) => {
          if (conn.connectionId !== mySession.connection?.connectionId) {
            setConnections((prev) => [...prev, conn]);
          }
        });
      }

      const pub = await OV.initPublisherAsync(undefined, {
        audioSource: undefined,
        videoSource: undefined,
        publishAudio: true,
        publishVideo: true,
        resolution: "640x480",
        frameRate: 30,
        insertMode: "APPEND",
      });

      setSession(mySession);
      setPublisher(pub);
      publisherRef.current = pub;

      // autoPublish가 true면 즉시 publish
      if (autoPublish) {
        mySession.publish(pub);
      }

      statusRef.current = "connected";
      setStatus("connected");
    } catch (e) {
      console.error(e);
      const message = e instanceof Error ? e.message : "연결에 실패했습니다";
      setError(message);
      statusRef.current = "error";
      setStatus("error");
      try {
        mySession.disconnect();
      } catch { }
    }
  }, [roomId, nickname, password, isOwner, memberId, roleId, autoPublish]);

  const publishStream = useCallback(() => {
    if (!session || !publisherRef.current) {
      return;
    }

    try {
      session.publish(publisherRef.current);
    } catch (error) {
      console.error('Failed to publish stream:', error);
      setError('스트림 전송에 실패했습니다');
    }
  }, [session]);

  const leave = useCallback(async () => {
    // 먼저 미디어 트랙 즉시 정리 (카메라/마이크 바로 끄기)
    const currentPublisher = publisherRef.current || publisher;
    const currentSession = session;

    if (currentPublisher) {
      // 1. Publisher의 audio/video 끄기
      try {
        currentPublisher.publishAudio(false);
        currentPublisher.publishVideo(false);
      } catch {
        // 이미 종료된 경우 무시
      }

      // 2. Session에서 unpublish
      if (currentSession) {
        try {
          currentSession.unpublish(currentPublisher);
        } catch {
          // 이미 unpublish된 경우 무시
        }
      }

      // 3. MediaStream의 모든 트랙 정리
      const stream = currentPublisher.stream?.getMediaStream();
      if (stream) {
        stream.getTracks().forEach(track => {
          track.stop();
        });
      }
    }

    // 화면에서 즉시 제거 (상태 초기화)
    ovRef.current = null;
    setPublisher(null);
    publisherRef.current = null;
    setSubscribers([]);
    setConnections([]); // ⭐ connections 배열 초기화 추가
    statusRef.current = "idle";
    setStatus("idle");
    setError(null);
    setIsAudioEnabled(true);
    setIsVideoEnabled(true);

    try {
      // 먼저 클라이언트 세션 정리 (이벤트 리스너 해제 및 연결 종료)
      if (currentSession) {
        try {
          currentSession.disconnect();
        } catch (error) {
          console.error('[useVideoRoom] Failed to disconnect session:', error);
        }
      }

      // 그 다음 백엔드 세션 삭제
      if (backendSessionId) {
        await deleteSession(backendSessionId);
      }
    } catch (error) {
      console.error('[useVideoRoom] Failed to leave WebRTC:', error);
    } finally {
      setSession(null);
      setBackendSessionId(null);
    }
  }, [publisher, session, backendSessionId]);

  const toggleAudio = useCallback(() => {
    if (!publisher) return;
    const newState = !isAudioEnabled;
    publisher.publishAudio(newState);
    setIsAudioEnabled(newState);
  }, [publisher, isAudioEnabled]);

  const toggleVideo = useCallback(() => {
    if (!publisher) return;
    const newState = !isVideoEnabled;
    publisher.publishVideo(newState);
    setIsVideoEnabled(newState);
  }, [publisher, isVideoEnabled]);

  useEffect(() => {
    if (autoJoin) {
      join();
    }
    return () => {
      // Cleanup: 미디어 트랙 정리
      if (publisherRef.current) {
        const stream = publisherRef.current.stream?.getMediaStream();
        if (stream) {
          stream.getTracks().forEach(track => {
            track.stop();
          });
        }
      }

      if (session) {
        try {
          session.disconnect();
        } catch { }
      }
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return {
    status,
    error,
    tiles,
    publisher,
    subscribers,
    isAudioEnabled,
    isVideoEnabled,
    join,
    leave,
    publishStream,
    toggleAudio,
    toggleVideo,
  };
}
