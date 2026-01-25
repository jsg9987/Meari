package com.ssafy.meari.domain.room.controller;

import com.ssafy.meari.domain.room.dto.request.RoomCreateRequest;
import com.ssafy.meari.domain.room.dto.request.RoomEnterRequest;
import com.ssafy.meari.domain.room.dto.response.RoomDetailResponse;
import com.ssafy.meari.domain.room.dto.response.RoomListResponse;
import com.ssafy.meari.domain.room.dto.response.RoomResponse;
import com.ssafy.meari.domain.room.service.RoomService;
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
import org.springframework.web.bind.annotation.*;

@Tag(name = "Room", description = "쉐도잉 방 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;

    // TODO: JWT 인증 구현 후 @AuthenticationPrincipal로 변경
    // 현재는 임시로 헤더에서 memberId를 받음
    private static final String TEMP_MEMBER_ID_HEADER = "X-Member-Id";

    @Operation(summary = "방 생성", description = "새로운 쉐도잉 방을 생성합니다. 생성자는 자동으로 방장이 됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @RequestHeader(TEMP_MEMBER_ID_HEADER) Long memberId,
            @Valid @RequestBody RoomCreateRequest request
    ) {
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
            @RequestHeader(TEMP_MEMBER_ID_HEADER) Long memberId,
            @RequestBody(required = false) RoomEnterRequest request
    ) {
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
            @RequestHeader(TEMP_MEMBER_ID_HEADER) Long memberId
    ) {
        log.info("방 퇴장 요청: roomId={}, memberId={}", roomId, memberId);
        roomService.leaveRoom(roomId, memberId);
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }
}
