import { useEffect, useMemo, useRef, useState, useCallback } from "react";
import { OpenVidu, Publisher, Session, Subscriber } from "openvidu-browser";
import { getToken } from "../api/webrtc.api";

export type ConnectionStatus = "idle" | "connecting" | "connected" | "error";

export interface VideoTileData {
  id: string;
  streamManager: Publisher | Subscriber;
  muted?: boolean;
  label?: string;
  isSpeaker?: boolean;
}

interface UseVideoRoomOptions {
  sessionName: string;
  nickname: string;
  autoJoin?: boolean;
}

export function useVideoRoom({ sessionName, nickname, autoJoin = false }: UseVideoRoomOptions) {
  const [session, setSession] = useState<Session | null>(null);
  const [publisher, setPublisher] = useState<Publisher | null>(null);
  const [subscribers, setSubscribers] = useState<Subscriber[]>([]);
  const [status, setStatus] = useState<ConnectionStatus>("idle");
  const [error, setError] = useState<string | null>(null);
  const [isAudioEnabled, setIsAudioEnabled] = useState(true);
  const [isVideoEnabled, setIsVideoEnabled] = useState(true);

  const ovRef = useRef<OpenVidu | null>(null);

  const tiles = useMemo<VideoTileData[]>(() => {
    const arr: VideoTileData[] = [];
    if (publisher) {
      arr.push({ id: "me", streamManager: publisher, muted: true, label: `${nickname} (나)` });
    }
    subscribers.forEach((s) => {
      const clientData = s.stream.connection.data;
      let name = "참여자";
      try {
        const parsed = JSON.parse(clientData);
        name = parsed.clientData || name;
      } catch {
        name = clientData || name;
      }
      arr.push({ id: s.stream.streamId, streamManager: s, label: name });
    });
    return arr;
  }, [publisher, subscribers, nickname]);

  const join = useCallback(async () => {
    if (status === "connecting" || status === "connected") return;

    setStatus("connecting");
    setError(null);

    const OV = new OpenVidu();
    ovRef.current = OV;
    const mySession = OV.initSession();

    mySession.on("streamCreated", (event) => {
      const subscriber = mySession.subscribe(event.stream, undefined);
      setSubscribers((prev) => [...prev, subscriber]);
    });

    mySession.on("streamDestroyed", (event) => {
      const streamId = event.stream.streamId;
      setSubscribers((prev) => prev.filter((s) => s.stream.streamId !== streamId));
    });

    mySession.on("exception", (ex) => {
      console.warn("OpenVidu exception", ex);
    });

    try {
      const token = await getToken(sessionName);
      await mySession.connect(token, { clientData: nickname });

      const pub = await OV.initPublisherAsync(undefined, {
        audioSource: undefined,
        videoSource: undefined,
        publishAudio: true,
        publishVideo: true,
        resolution: "640x480",
        frameRate: 30,
        insertMode: "APPEND",
      });

      mySession.publish(pub);

      setSession(mySession);
      setPublisher(pub);
      setStatus("connected");
    } catch (e) {
      console.error(e);
      const message = e instanceof Error ? e.message : "연결에 실패했습니다";
      setError(message);
      setStatus("error");
      try {
        mySession.disconnect();
      } catch {}
    }
  }, [sessionName, nickname, status]);

  const leave = useCallback(() => {
    try {
      session?.disconnect();
    } finally {
      ovRef.current = null;
      setSession(null);
      setPublisher(null);
      setSubscribers([]);
      setStatus("idle");
      setError(null);
      setIsAudioEnabled(true);
      setIsVideoEnabled(true);
    }
  }, [session]);

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
      if (session) {
        try {
          session.disconnect();
        } catch {}
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
    toggleAudio,
    toggleVideo,
  };
}
