package com.ssafy.meari.domain.webrtc.service;

import com.ssafy.meari.domain.webrtc.dto.request.OpenViduConnectionRequest;
import com.ssafy.meari.domain.webrtc.dto.request.OpenViduSessionRequest;
import com.ssafy.meari.domain.webrtc.dto.response.OpenViduConnectionResponse;
import com.ssafy.meari.domain.webrtc.dto.response.OpenViduSessionResponse;
import com.ssafy.meari.domain.webrtc.dto.response.SessionInfoResponse;
import com.ssafy.meari.domain.webrtc.dto.response.SessionListResponse;
import com.ssafy.meari.global.error.ErrorCode;
import com.ssafy.meari.global.error.exception.BusinessException;
import io.openvidu.java.client.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OpenViduService {

    private final OpenVidu openVidu;

    // 세션 ID와 Session 객체 매핑 (캐싱)
    private final Map<String, Session> sessionCache = new ConcurrentHashMap<>();

    /**
     * 세션 생성
     */
    public OpenViduSessionResponse createSession(OpenViduSessionRequest request) {
        log.debug("OpenVidu 세션 생성 요청: customSessionId={}", request.getCustomSessionId());

        try {
            SessionProperties.Builder propertiesBuilder = new SessionProperties.Builder();

            // 커스텀 세션 ID가 있는 경우 설정
            if (request.getCustomSessionId() != null && !request.getCustomSessionId().isEmpty()) {
                propertiesBuilder.customSessionId(request.getCustomSessionId());
            }

            Session session = openVidu.createSession(propertiesBuilder.build());
            String sessionId = session.getSessionId();

            // 세션 캐시에 저장
            sessionCache.put(sessionId, session);

            log.debug("OpenVidu 세션 생성 완료: sessionId={}", sessionId);
            return OpenViduSessionResponse.of(sessionId);

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("OpenVidu 세션 생성 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENVIDU_SESSION_CREATE_FAILED);
        }
    }

    /**
     * 연결 토큰 생성
     */
    public OpenViduConnectionResponse createConnection(String sessionId, OpenViduConnectionRequest request) {
        log.debug("OpenVidu 연결 토큰 생성 요청: sessionId={}, memberId={}", sessionId, request.getMemberId());

        Session session = getOrFetchSession(sessionId);

        try {
            // 클라이언트 데이터 구성 (JSON 형식)
            String clientData = buildClientData(request);

            ConnectionProperties properties = new ConnectionProperties.Builder()
                    .type(ConnectionType.WEBRTC)
                    .data(clientData)
                    .build();

            Connection connection = session.createConnection(properties);

            log.debug("OpenVidu 연결 토큰 생성 완료: connectionId={}", connection.getConnectionId());
            return OpenViduConnectionResponse.of(
                    sessionId,
                    connection.getToken(),
                    connection.getConnectionId()
            );

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("OpenVidu 연결 생성 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENVIDU_CONNECTION_CREATE_FAILED);
        }
    }

    /**
     * 세션 정보 조회
     */
    public SessionInfoResponse getSessionInfo(String sessionId) {
        log.debug("OpenVidu 세션 정보 조회: sessionId={}", sessionId);

        Session session = getOrFetchSession(sessionId);

        try {
            // 세션 정보 새로고침
            session.fetch();

            List<SessionInfoResponse.ConnectionInfo> connections = session.getActiveConnections().stream()
                    .map(conn -> SessionInfoResponse.ConnectionInfo.of(
                            conn.getConnectionId(),
                            conn.createdAt(),
                            conn.getPlatform(),
                            conn.getClientData()
                    ))
                    .collect(Collectors.toList());

            return SessionInfoResponse.of(
                    session.getSessionId(),
                    session.createdAt(),
                    connections
            );

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("OpenVidu 세션 정보 조회 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENVIDU_FETCH_FAILED);
        }
    }

    /**
     * 활성 세션 목록 조회
     */
    public List<SessionListResponse> getActiveSessions() {
        log.debug("OpenVidu 활성 세션 목록 조회");

        try {
            openVidu.fetch();

            return openVidu.getActiveSessions().stream()
                    .map(session -> SessionListResponse.of(session.getSessionId()))
                    .collect(Collectors.toList());

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("OpenVidu 세션 목록 조회 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENVIDU_FETCH_FAILED);
        }
    }

    /**
     * 세션 종료
     */
    public void closeSession(String sessionId) {
        log.debug("OpenVidu 세션 종료 요청: sessionId={}", sessionId);

        Session session = getOrFetchSession(sessionId);

        try {
            session.close();
            sessionCache.remove(sessionId);
            log.debug("OpenVidu 세션 종료 완료: sessionId={}", sessionId);

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("OpenVidu 세션 종료 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENVIDU_SESSION_CLOSE_FAILED);
        }
    }

    /**
     * 연결 강제 종료
     */
    public void forceDisconnect(String sessionId, String connectionId) {
        log.debug("OpenVidu 연결 강제 종료 요청: sessionId={}, connectionId={}", sessionId, connectionId);

        Session session = getOrFetchSession(sessionId);

        try {
            session.forceDisconnect(connectionId);
            log.debug("OpenVidu 연결 강제 종료 완료: connectionId={}", connectionId);

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("OpenVidu 연결 강제 종료 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENVIDU_DISCONNECT_FAILED);
        }
    }

    /**
     * 방 입장용 세션 가져오기 또는 생성
     * 세션이 없으면 새로 생성하고, 있으면 기존 세션 반환
     */
    public Session getOrCreateSession(Long roomId) {
        String sessionId = "room_" + roomId;
        log.debug("OpenVidu 세션 조회/생성: sessionId={}", sessionId);

        // 캐시에서 먼저 확인
        Session cachedSession = sessionCache.get(sessionId);
        if (cachedSession != null) {
            try {
                cachedSession.fetch();
                return cachedSession;
            } catch (OpenViduJavaClientException | OpenViduHttpException e) {
                // 세션이 만료되었을 수 있음, 캐시에서 제거
                sessionCache.remove(sessionId);
            }
        }

        // OpenVidu 서버에서 조회
        try {
            openVidu.fetch();
            for (Session session : openVidu.getActiveSessions()) {
                if (sessionId.equals(session.getSessionId())) {
                    sessionCache.put(sessionId, session);
                    return session;
                }
            }
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.warn("OpenVidu 세션 목록 조회 실패, 새 세션 생성 시도: {}", e.getMessage());
        }

        // 세션이 없으면 새로 생성
        try {
            SessionProperties properties = new SessionProperties.Builder()
                    .customSessionId(sessionId)
                    .build();
            Session session = openVidu.createSession(properties);
            sessionCache.put(sessionId, session);
            log.debug("새 OpenVidu 세션 생성 완료: sessionId={}", sessionId);
            return session;

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("OpenVidu 세션 생성 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENVIDU_SESSION_CREATE_FAILED);
        }
    }

    /**
     * 방 입장용 연결 토큰 생성
     */
    public OpenViduConnectionResponse createConnectionForRoom(Long roomId, Long memberId, String nickname) {
        log.debug("방 입장용 OpenVidu 연결 생성: roomId={}, memberId={}", roomId, memberId);

        Session session = getOrCreateSession(roomId);

        try {
            String clientData = String.format("{\"memberId\":%d,\"nickname\":\"%s\"}", memberId, nickname);

            ConnectionProperties properties = new ConnectionProperties.Builder()
                    .type(ConnectionType.WEBRTC)
                    .data(clientData)
                    .build();

            Connection connection = session.createConnection(properties);

            return OpenViduConnectionResponse.of(
                    session.getSessionId(),
                    connection.getToken(),
                    connection.getConnectionId()
            );

        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("OpenVidu 연결 생성 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENVIDU_CONNECTION_CREATE_FAILED);
        }
    }

    /**
     * 세션 조회 (캐시 우선, 없으면 서버에서 조회)
     */
    private Session getOrFetchSession(String sessionId) {
        // 캐시에서 먼저 확인
        Session cachedSession = sessionCache.get(sessionId);
        if (cachedSession != null) {
            return cachedSession;
        }

        // 서버에서 조회
        try {
            openVidu.fetch();
            for (Session session : openVidu.getActiveSessions()) {
                if (sessionId.equals(session.getSessionId())) {
                    sessionCache.put(sessionId, session);
                    return session;
                }
            }
        } catch (OpenViduJavaClientException | OpenViduHttpException e) {
            log.error("OpenVidu 세션 조회 실패: {}", e.getMessage());
            throw new BusinessException(ErrorCode.OPENVIDU_FETCH_FAILED);
        }

        throw new BusinessException(ErrorCode.OPENVIDU_SESSION_NOT_FOUND);
    }

    /**
     * 클라이언트 데이터 JSON 문자열 생성
     */
    private String buildClientData(OpenViduConnectionRequest request) {
        StringBuilder sb = new StringBuilder("{");
        boolean hasContent = false;

        if (request.getMemberId() != null) {
            sb.append("\"memberId\":").append(request.getMemberId());
            hasContent = true;
        }

        if (request.getNickname() != null) {
            if (hasContent) sb.append(",");
            sb.append("\"nickname\":\"").append(request.getNickname()).append("\"");
            hasContent = true;
        }

        if (request.getRoleId() != null) {
            if (hasContent) sb.append(",");
            sb.append("\"roleId\":").append(request.getRoleId());
        }

        sb.append("}");
        return sb.toString();
    }
}
