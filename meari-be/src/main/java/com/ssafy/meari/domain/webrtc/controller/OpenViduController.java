package com.ssafy.meari.domain.webrtc.controller;

import com.ssafy.meari.domain.webrtc.dto.request.OpenViduConnectionRequest;
import com.ssafy.meari.domain.webrtc.dto.request.OpenViduSessionRequest;
import com.ssafy.meari.domain.webrtc.dto.response.OpenViduConnectionResponse;
import com.ssafy.meari.domain.webrtc.dto.response.OpenViduSessionResponse;
import com.ssafy.meari.domain.webrtc.dto.response.SessionInfoResponse;
import com.ssafy.meari.domain.webrtc.dto.response.SessionListResponse;
import com.ssafy.meari.domain.webrtc.service.OpenViduService;
import com.ssafy.meari.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "🛠️ Development", description = "개발/테스트 전용 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/openvidu")
@RequiredArgsConstructor
public class OpenViduController {

    private final OpenViduService openViduService;

    @Operation(summary = "세션 생성", description = "새로운 OpenVidu 세션을 생성합니다.")
    @PostMapping("/sessions")
    public ResponseEntity<ApiResponse<OpenViduSessionResponse>> createSession(
            @RequestBody(required = false) OpenViduSessionRequest request
    ) {
        log.info("OpenVidu 세션 생성 요청");
        if (request == null) {
            request = new OpenViduSessionRequest();
        }
        OpenViduSessionResponse response = openViduService.createSession(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "연결 토큰 생성", description = "특정 세션에 참여할 수 있는 토큰을 발급합니다.")
    @PostMapping("/sessions/{sessionId}/connections")
    public ResponseEntity<ApiResponse<OpenViduConnectionResponse>> createConnection(
            @Parameter(description = "세션 ID") @PathVariable String sessionId,
            @RequestBody(required = false) OpenViduConnectionRequest request
    ) {
        log.info("OpenVidu 연결 토큰 생성 요청: sessionId={}", sessionId);
        if (request == null) {
            request = new OpenViduConnectionRequest();
        }
        OpenViduConnectionResponse response = openViduService.createConnection(sessionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "세션 정보 조회", description = "세션의 상세 정보와 연결된 참여자 목록을 조회합니다.")
    @GetMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<SessionInfoResponse>> getSessionInfo(
            @Parameter(description = "세션 ID") @PathVariable String sessionId
    ) {
        log.info("OpenVidu 세션 정보 조회 요청: sessionId={}", sessionId);
        SessionInfoResponse response = openViduService.getSessionInfo(sessionId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "활성 세션 목록 조회", description = "현재 활성화된 모든 세션의 목록을 조회합니다.")
    @GetMapping("/sessions")
    public ResponseEntity<ApiResponse<List<SessionListResponse>>> getActiveSessions() {
        log.info("OpenVidu 활성 세션 목록 조회 요청");
        List<SessionListResponse> response = openViduService.getActiveSessions();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "세션 종료", description = "세션을 종료하고 모든 연결을 해제합니다.")
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<Void>> closeSession(
            @Parameter(description = "세션 ID") @PathVariable String sessionId
    ) {
        log.info("OpenVidu 세션 종료 요청: sessionId={}", sessionId);
        openViduService.closeSession(sessionId);
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "연결 강제 종료", description = "특정 사용자를 세션에서 강제로 퇴장시킵니다.")
    @DeleteMapping("/sessions/{sessionId}/connections/{connectionId}")
    public ResponseEntity<ApiResponse<Void>> forceDisconnect(
            @Parameter(description = "세션 ID") @PathVariable String sessionId,
            @Parameter(description = "연결 ID") @PathVariable String connectionId
    ) {
        log.info("OpenVidu 연결 강제 종료 요청: sessionId={}, connectionId={}", sessionId, connectionId);
        openViduService.forceDisconnect(sessionId, connectionId);
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }
}
