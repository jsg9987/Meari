package com.ssafy.meari.domain.webrtc.controller;

import com.ssafy.meari.domain.webrtc.dto.request.RoomEnterWebRtcRequest;
import com.ssafy.meari.domain.webrtc.dto.response.RoomEnterWebRtcResponse;
import com.ssafy.meari.domain.webrtc.service.WebRtcRoomService;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.common.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "8. WebRTC", description = "화상통화 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class WebRtcRoomController {

    private final WebRtcRoomService webRtcRoomService;

    @Operation(summary = "방 입장 (WebRTC)", description = "방 입장 시 비밀번호와 정원을 확인한 후 WebRTC 토큰을 발급합니다.")
    @PostMapping("/{roomId}/webrtc/enter")
    public ResponseEntity<ApiResponse<RoomEnterWebRtcResponse>> enterRoomWithWebRtc(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody(required = false) RoomEnterWebRtcRequest request
    ) {
        if (request == null) {
            request = new RoomEnterWebRtcRequest();
        }

        Long memberId = userDetails.getMember().getMemberId();
        String nickname = request.getNickname();

        log.info("방 입장 (WebRTC) 요청: roomId={}, memberId={}", roomId, memberId);

        RoomEnterWebRtcResponse response = webRtcRoomService.enterRoomWithWebRtc(
                roomId, request.getPassword(), memberId, nickname);

        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "방 퇴장 (WebRTC)", description = "방에서 퇴장합니다.")
    @DeleteMapping("/{roomId}/webrtc/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoomWithWebRtc(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("방 퇴장 (WebRTC) 요청: roomId={}, memberId={}", roomId, memberId);
        webRtcRoomService.leaveRoomWithWebRtc(roomId, memberId);
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

}
