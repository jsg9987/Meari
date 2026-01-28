package com.ssafy.meari.domain.room.controller;

import com.ssafy.meari.domain.room.dto.request.ContentSelectRequest;
import com.ssafy.meari.domain.room.dto.request.GameStartRequest;
import com.ssafy.meari.domain.room.dto.request.RoleSelectRequest;
import com.ssafy.meari.domain.room.dto.request.RoomCreateRequest;
import com.ssafy.meari.domain.room.dto.request.RoomEnterRequest;
import com.ssafy.meari.domain.room.dto.response.RoomDetailResponse;
import com.ssafy.meari.domain.room.dto.response.RoomListResponse;
import com.ssafy.meari.domain.room.dto.response.RoomResponse;
import com.ssafy.meari.domain.room.service.RoomService;
import com.ssafy.meari.global.auth.UserDetailsImpl;
import com.ssafy.meari.global.common.ApiResponse;
import com.ssafy.meari.global.common.CursorPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Room", description = "쉐도잉 방 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    @Operation(summary = "방 생성", description = "새로운 쉐도잉 방을 생성합니다. 생성자는 자동으로 방장이 됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody RoomCreateRequest request
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("방 생성 요청: memberId={}, title={}", memberId, request.getTitle());
        RoomResponse response = roomService.createRoom(request, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "방 목록 조회", description = "커서 기반 페이징으로 방 목록을 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<CursorPageResponse<RoomListResponse>>> getRoomList(
            @Parameter(description = "테마 ID (선택, 없으면 전체 조회)")
            @RequestParam(required = false) Long themeId,
            @Parameter(description = "커서 (이전 페이지 마지막 roomId)")
            @RequestParam(required = false) Long cursor,
            @Parameter(description = "페이지 크기 (기본값: 10)")
            @RequestParam(defaultValue = "10") int size
    ) {
        log.info("방 목록 조회 요청: themeId={}, cursor={}, size={}", themeId, cursor, size);
        CursorPageResponse<RoomListResponse> response = roomService.getRoomList(themeId, cursor, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "방 상세 조회", description = "방의 상세 정보와 참여자 목록을 조회합니다.")
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> getRoomDetail(
            @Parameter(description = "방 ID") @PathVariable Long roomId
    ) {
        log.info("방 상세 조회 요청: roomId={}", roomId);
        RoomDetailResponse response = roomService.getRoomDetail(roomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "방 입장", description = "방에 입장합니다. 비밀방인 경우 비밀번호가 필요합니다.")
    @PostMapping("/{roomId}/enter")
    public ResponseEntity<ApiResponse<Void>> enterRoom(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody(required = false) RoomEnterRequest request
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("방 입장 요청: roomId={}, memberId={}", roomId, memberId);
        if (request == null) {
            request = new RoomEnterRequest();
        }
        roomService.enterRoom(roomId, request, memberId);
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "방 퇴장", description = "방에서 퇴장합니다. 방장이 퇴장하면 다음 사람에게 방장이 위임됩니다.")
    @DeleteMapping("/{roomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("방 퇴장 요청: roomId={}, memberId={}", roomId, memberId);
        roomService.leaveRoom(roomId, memberId);
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "준비 상태 토글", description = "준비 상태를 토글합니다. 방장은 사용할 수 없습니다.")
    @PostMapping("/{roomId}/ready")
    public ResponseEntity<ApiResponse<Boolean>> toggleReady(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("준비 상태 토글 요청: roomId={}, memberId={}", roomId, memberId);
        boolean ready = roomService.toggleReady(roomId, memberId);
        return ResponseEntity.ok(ApiResponse.success(ready));
    }

    @Operation(summary = "동영상 선택", description = "학습할 동영상을 선택합니다. 방장만 가능하며, WAITING 단계에서만 가능합니다.")
    @PostMapping("/{roomId}/content")
    public ResponseEntity<ApiResponse<Void>> selectContent(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ContentSelectRequest request
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("동영상 선택 요청: roomId={}, contentId={}, memberId={}", roomId, request.getContentId(), memberId);
        roomService.selectContent(roomId, request.getContentId(), memberId);
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "게임 시작", description = "게임을 시작합니다. 방장만 가능하며, 동영상을 선택하고 모든 참여자가 준비 완료 상태여야 합니다.")
    @PostMapping("/{roomId}/start")
    public ResponseEntity<ApiResponse<Void>> startGame(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody GameStartRequest request
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("게임 시작 요청: roomId={}, contentId={}, memberId={}", roomId, request.getContentId(), memberId);
        roomService.startGame(roomId, request.getContentId(), memberId);
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "역할 선점", description = "역할을 선점합니다. ROLE_PICK 단계에서만 가능합니다.")
    @PostMapping("/{roomId}/role")
    public ResponseEntity<ApiResponse<Void>> selectRole(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody RoleSelectRequest request
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        log.info("역할 선점 요청: roomId={}, roleId={}, memberId={}", roomId, request.getRoleId(), memberId);
        roomService.selectRole(roomId, request.getRoleId(), memberId);
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }
}
