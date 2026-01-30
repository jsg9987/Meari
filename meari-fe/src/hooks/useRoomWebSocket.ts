import { useEffect, useRef, useCallback, useState } from 'react';
import { Client, type IMessage, type StompSubscription } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

// 웹소켓 메시지 타입 정의
export type WebSocketMessageType =
  | 'MEMBER_JOIN'
  | 'READY'
  | 'ROLE_PICK'
  | 'ROLE_ASSIGNED'
  | 'ROLE_RELEASED'
  | 'GAME_START'
  | 'PHASE_CHANGE'
  | 'ROLES_CONFIRMED'
  | 'ROUND_START'
  | 'GAME_FINISHED';

// 역할(캐릭터) 정보
export interface Role {
  id: number;
  role_id: number;
  name: string;
  created_at: string;
  updated_at: string;
}

// 대본 문장 정보
export interface Sentence {
  sentence_id: number;
  sequence: number;
  start_time: number;
  end_time: number;
  text_ko: string;
  text_vn: string;
}

// 역할별 대본 세그먼트
export interface RoleSegment {
  member_id: number;
  role_id: number;
  role_name: string;
  sentences: Sentence[];
}

// 웹소켓 메시지 페이로드
export interface WebSocketMessage {
  type: WebSocketMessageType;
  member_id?: number;
  ready?: boolean;
  role_id?: number | null;
  phase?: string | null;
  nickname?: string | null;
  new_owner_id?: number | null;
  content_id?: number | null;
  round?: number | null;
  server_time?: number | null;
  roles?: Role[]; // ROLE_PICK 메시지에서 사용
  segments?: RoleSegment[]; // ROLES_CONFIRMED 및 ROUND_START 메시지에서 사용
}

// 준비 상태 변경 요청
export interface ReadyToggleRequest {
  member_id: number;
}

// 역할 선점/해제 요청
export interface RoleRequest {
  role_id: number;
  member_id: number;
}

// 멤버 참가자 정보
export interface RoomMember {
  member_id: number;
  nickname: string;
  ready: boolean;
  role_id: number | null;
}

interface UseRoomWebSocketOptions {
  roomId: number;
  memberId: number;
  onMessage?: (message: WebSocketMessage) => void;
  onMemberJoin?: (message: WebSocketMessage) => void;
  onReady?: (message: WebSocketMessage) => void;
  onRolePick?: (message: WebSocketMessage) => void;
  onRoleAssigned?: (message: WebSocketMessage) => void;
  onRoleReleased?: (message: WebSocketMessage) => void;
  onGameStart?: (message: WebSocketMessage) => void;
  onPhaseWaiting?: (message: WebSocketMessage) => void;
  onRolesConfirmed?: (message: WebSocketMessage) => void;
  onRoundStart?: (message: WebSocketMessage) => void;
  onGameFinished?: (message: WebSocketMessage) => void;
  onConnect?: () => void;
  onDisconnect?: () => void;
  onError?: (error: Error) => void;
}

export function useRoomWebSocket({
  roomId,
  memberId,
  onMessage,
  onMemberJoin,
  onReady,
  onRolePick,
  onRoleAssigned,
  onRoleReleased,
  onGameStart,
  onPhaseWaiting,
  onRolesConfirmed,
  onRoundStart,
  onGameFinished,
  onConnect,
  onDisconnect,
  onError,
}: UseRoomWebSocketOptions) {
  const clientRef = useRef<Client | null>(null);
  const subscriptionsRef = useRef<StompSubscription[]>([]);
  const [isConnected, setIsConnected] = useState(false);
  const [connectionError, setConnectionError] = useState<string | null>(null);

  // 웹소켓 URL 생성 (SockJS 엔드포인트)
  const getWebSocketUrl = useCallback(() => {
    const baseUrl = import.meta.env.VITE_BASE_SERVER_URL || 'http://localhost:8080/api/v1';
    // /api/v1 제거하고 /ws 엔드포인트로 변경
    const wsBaseUrl = baseUrl.replace('/api/v1', '');
    return `${wsBaseUrl}/ws`;
  }, []);

  // 메시지 핸들러
  const handleMessage = useCallback((message: IMessage) => {
    try {
      const payload: WebSocketMessage = JSON.parse(message.body);
      console.log('[WebSocket] Received message:', payload);
      console.log('[WebSocket] Message type:', payload.type);

      // 공통 핸들러 호출
      onMessage?.(payload);

      // 타입별 핸들러 호출
      switch (payload.type) {
        case 'MEMBER_JOIN':
          console.log('[WebSocket] Handling MEMBER_JOIN');
          onMemberJoin?.(payload);
          break;
        case 'READY':
          console.log('[WebSocket] Handling READY');
          onReady?.(payload);
          break;
        case 'ROLE_PICK':
          console.log('[WebSocket] Handling ROLE_PICK');
          console.log('[WebSocket] ROLE_PICK payload:', JSON.stringify(payload, null, 2));
          onRolePick?.(payload);
          break;
        case 'PHASE_CHANGE':
          console.log('[WebSocket] Handling PHASE_CHANGE');
          console.log('[WebSocket] Phase:', payload.phase);
          // PHASE_CHANGE 타입에서 phase가 ROLE_PICK일 때 역할 선택 처리
          if (payload.phase === 'ROLE_PICK') {
            console.log('[WebSocket] ROLE_PICK phase detected');
            console.log('[WebSocket] ROLE_PICK payload:', JSON.stringify(payload, null, 2));
            onRolePick?.(payload);
          } else if (payload.phase === 'WATCHING') {
            // 영상 시청 페이즈 (게임 시작)
            console.log('[WebSocket] WATCHING phase - calling onGameStart');
            onGameStart?.(payload);
          } else if (payload.phase === 'WAITING') {
            // 대기 페이즈 (처음으로 돌아가기)
            console.log('[WebSocket] WAITING phase - resetting to initial state');
            onPhaseWaiting?.(payload);
          }
          break;
        case 'ROLE_ASSIGNED':
          console.log('[WebSocket] Handling ROLE_ASSIGNED');
          onRoleAssigned?.(payload);
          break;
        case 'ROLE_RELEASED':
          console.log('[WebSocket] Handling ROLE_RELEASED');
          onRoleReleased?.(payload);
          break;
        case 'GAME_START':
          console.log('[WebSocket] Handling GAME_START');
          onGameStart?.(payload);
          break;
        case 'ROLES_CONFIRMED':
          console.log('[WebSocket] Handling ROLES_CONFIRMED');
          console.log('[WebSocket] Segments:', payload.segments);
          onRolesConfirmed?.(payload);
          break;
        case 'ROUND_START':
          console.log('[WebSocket] Handling ROUND_START');
          console.log('[WebSocket] Round:', payload.round, 'Server time:', payload.server_time);
          console.log('[WebSocket] Segments:', payload.segments);
          onRoundStart?.(payload);
          break;
        case 'GAME_FINISHED':
          console.log('[WebSocket] Handling GAME_FINISHED');
          onGameFinished?.(payload);
          break;
        default:
          console.warn('[WebSocket] Unknown message type:', payload.type);
      }
    } catch (error) {
      console.error('[WebSocket] Failed to parse message:', error);
      console.error('[WebSocket] Raw message:', message.body);
    }
  }, [onMessage, onMemberJoin, onReady, onRolePick, onRoleAssigned, onRoleReleased, onGameStart, onPhaseWaiting, onRolesConfirmed, onRoundStart, onGameFinished]);

  // 웹소켓 연결
  const connect = useCallback(() => {
    if (clientRef.current?.connected) {
      console.log('[WebSocket] Already connected');
      return;
    }

    // JWT 토큰 가져오기
    const accessToken = localStorage.getItem('accessToken');

    // WebSocket URL에 토큰 추가 (SockJS는 URL 파라미터로 토큰을 전달해야 함)
    let wsUrl = getWebSocketUrl();
    if (accessToken) {
      wsUrl = `${wsUrl}?token=${encodeURIComponent(accessToken)}`;
    }

    console.log('[WebSocket] Connecting to:', wsUrl.replace(/token=[^&]+/, 'token=***'));

    // SockJS를 사용한 WebSocket 연결
    const client = new Client({
      webSocketFactory: () => new SockJS(wsUrl) as WebSocket,
      reconnectDelay: 5000,
      heartbeatIncoming: 4000,
      heartbeatOutgoing: 4000,
      debug: (str) => {
        console.log('[WebSocket Debug]', str);
      },

      beforeConnect: async () => {
        const token = localStorage.getItem('access_token');
        if (!token) throw new Error('No access token');
        client.connectHeaders = { Authorization: `Bearer ${token}` };
      },

      onConnect: () => {
        console.log('[WebSocket] Connected');
        setIsConnected(true);
        setConnectionError(null);
        onConnect?.();

        // 구독 설정
        try {
          // 1. 방의 모든 실시간 이벤트 구독 (MEMBER_JOIN 등)
          const roomSub = client.subscribe(
            `/topic/rooms/${roomId}`,
            handleMessage
          );
          subscriptionsRef.current.push(roomSub);

          // 2. 방 상태 변경 이벤트 구독 (READY, GAME_START 등)
          const stateSub = client.subscribe(
            `/topic/room/${roomId}/state`,
            handleMessage
          );
          subscriptionsRef.current.push(stateSub);

          console.log('[WebSocket] Subscribed to /topic/rooms/' + roomId);
          console.log('[WebSocket] Subscribed to /topic/room/' + roomId + '/state');
        } catch (error) {
          console.error('[WebSocket] Subscription failed:', error);
          onError?.(error as Error);
        }
      },
      onDisconnect: () => {
        console.log('[WebSocket] Disconnected');
        setIsConnected(false);
        onDisconnect?.();
      },
      onStompError: (frame) => {
        console.error('[WebSocket] STOMP error:', frame);
        const errorMessage = frame.headers['message'] || 'STOMP connection error';
        setConnectionError(errorMessage);
        onError?.(new Error(errorMessage));
      },
      onWebSocketError: (event) => {
        console.error('[WebSocket] WebSocket error:', event);
        const errorMessage = 'WebSocket connection error';
        setConnectionError(errorMessage);
        onError?.(new Error(errorMessage));
      },
    });

    // JWT 토큰을 STOMP 연결 헤더에도 추가 (이중 보안)
    if (accessToken) {
      client.connectHeaders = {
        Authorization: `Bearer ${accessToken}`,
      };
    }

    clientRef.current = client;
    client.activate();
  }, [roomId, memberId, getWebSocketUrl, handleMessage, onConnect, onDisconnect, onError]);

  // 웹소켓 연결 해제
  const disconnect = useCallback(() => {
    if (!clientRef.current) return;

    console.log('[WebSocket] Disconnecting...');

    // 구독 해제
    subscriptionsRef.current.forEach((sub) => {
      try {
        sub.unsubscribe();
      } catch (error) {
        console.error('[WebSocket] Failed to unsubscribe:', error);
      }
    });
    subscriptionsRef.current = [];

    // 연결 해제
    try {
      clientRef.current.deactivate();
    } catch (error) {
      console.error('[WebSocket] Failed to disconnect:', error);
    }

    clientRef.current = null;
    setIsConnected(false);
    setConnectionError(null);
  }, []);

  // 준비 상태 토글
  const toggleReady = useCallback((ready: boolean) => {
    if (!clientRef.current?.connected) {
      console.warn('[WebSocket] Not connected, cannot toggle ready');
      return;
    }

    try {
      const payload: ReadyToggleRequest = {
        member_id: memberId,
      };

      clientRef.current.publish({
        destination: `/app/room/${roomId}/ready`,
        body: JSON.stringify(payload),
      });

      console.log('[WebSocket] Sent ready toggle:', ready);
    } catch (error) {
      console.error('[WebSocket] Failed to toggle ready:', error);
      onError?.(error as Error);
    }
  }, [roomId, memberId, onError]);

  // 역할 선점
  const assignRole = useCallback((roleId: number) => {
    if (!clientRef.current?.connected) {
      console.warn('[WebSocket] Not connected, cannot assign role');
      return;
    }

    try {
      const payload: RoleRequest = {
        role_id: roleId,
        member_id: memberId,
      };

      clientRef.current.publish({
        destination: `/app/room/${roomId}/role`,
        body: JSON.stringify(payload),
      });

      console.log('[WebSocket] Sent role assignment:', roleId);
    } catch (error) {
      console.error('[WebSocket] Failed to assign role:', error);
      onError?.(error as Error);
    }
  }, [roomId, memberId, onError]);

  // 역할 해제
  const releaseRole = useCallback((roleId: number) => {
    if (!clientRef.current?.connected) {
      console.warn('[WebSocket] Not connected, cannot release role');
      return;
    }

    try {
      const payload: RoleRequest = {
        role_id: roleId,
        member_id: memberId,
      };

      clientRef.current.publish({
        destination: `/app/rooms/${roomId}/roles/release`,
        body: JSON.stringify(payload),
      });

      console.log('[WebSocket] Sent role release:', roleId);
    } catch (error) {
      console.error('[WebSocket] Failed to release role:', error);
      onError?.(error as Error);
    }
  }, [roomId, memberId, onError]);

  // 컴포넌트 마운트 시 연결, 언마운트 시 해제
  useEffect(() => {
    connect();

    return () => {
      disconnect();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [roomId, memberId]); // roomId와 memberId가 변경될 때만 재연결

  return {
    isConnected,
    connectionError,
    connect,
    disconnect,
    toggleReady,
    assignRole,
    releaseRole,
  };
}
