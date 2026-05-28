package com.ssafy.meari.domain.room.controller;

import com.ssafy.meari.domain.room.dto.request.ContentSelectRequest;
import com.ssafy.meari.domain.room.dto.request.GameStartRequest;
import com.ssafy.meari.domain.room.dto.request.RoleConfirmRequest;
import com.ssafy.meari.domain.room.dto.request.RoundStartRequest;
import com.ssafy.meari.domain.room.dto.request.QuickRoomCreateRequest;
import com.ssafy.meari.domain.room.dto.request.RoomCreateRequest;
import com.ssafy.meari.domain.room.dto.request.RoomEnterRequest;
import com.ssafy.meari.domain.room.dto.response.RoomDetailResponse;
import com.ssafy.meari.domain.room.dto.response.RoomListResponse;
import com.ssafy.meari.domain.room.dto.response.RoomResponse;
import com.ssafy.meari.domain.room.service.RoomQueryService;
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

@Tag(name = "2. Room", description = "쉐도잉 방 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/v1/rooms")
@RequiredArgsConstructor
public class RoomController {

    private final RoomService roomService;
    private final RoomQueryService roomQueryService;

    @Operation(summary = "방 생성", description = "새로운 쉐도잉 방을 생성합니다. 생성자는 자동으로 방장이 됩니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<RoomResponse>> createRoom(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody RoomCreateRequest request
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        RoomResponse response = roomService.createRoom(request, memberId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
    }

    @Operation(summary = "빠른 방 생성", description = "테마 배너 클릭 시 빠른 방 생성. 랜덤 콘텐츠가 자동 선택되고 방이 즉시 생성됩니다.")
    @PostMapping("/quick")
    public ResponseEntity<ApiResponse<RoomResponse>> createQuickRoom(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody QuickRoomCreateRequest request
    ) {
        Long memberId = userDetails.getMember().getMemberId();
        RoomResponse response = roomService.createQuickRoom(request.getThemeId(), memberId);
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
        CursorPageResponse<RoomListResponse> response = roomQueryService.getRoomList(themeId, cursor, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "방 상세 조회", description = "방의 상세 정보와 참여자 목록을 조회합니다.")
    @GetMapping("/{roomId}")
    public ResponseEntity<ApiResponse<RoomDetailResponse>> getRoomDetail(
            @Parameter(description = "방 ID") @PathVariable Long roomId
    ) {
        log.info("방 상세 조회 요청: roomId={}", roomId);
        RoomDetailResponse response = roomQueryService.getRoomDetail(roomId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "방 입장", description = "방에 입장합니다. 비밀방인 경우 비밀번호가 필요합니다.")
    @PostMapping("/{roomId}/enter")
    public ResponseEntity<ApiResponse<Void>> enterRoom(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody(required = false) RoomEnterRequest request
    ) {
        // 방 입장 시 비밀번호가 없는 경우
        if (request == null) request = new RoomEnterRequest();

        roomService.enterRoom(roomId, request, userDetails.getMember().getMemberId());
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "방 퇴장", description = "방에서 퇴장합니다. 방장이 퇴장하면 다음 사람에게 방장이 위임됩니다.")
    @DeleteMapping("/{roomId}/leave")
    public ResponseEntity<ApiResponse<Void>> leaveRoom(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        roomService.leaveRoom(roomId, userDetails.getMember().getMemberId());
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "멤버 강퇴", description = "방장이 특정 멤버를 강퇴합니다. WAITING 상태에서만 가능합니다.")
    @DeleteMapping("/{roomId}/members/{memberId}")
    public ResponseEntity<ApiResponse<Void>> kickMember(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @Parameter(description = "강퇴 대상 멤버 ID") @PathVariable Long memberId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        roomService.kickMember(roomId, memberId, userDetails.getMember().getMemberId());
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "동영상 선택", description = "학습할 동영상을 선택합니다. 방장만 가능하며, WAITING 단계에서만 가능합니다.")
    @PostMapping("/{roomId}/content")
    public ResponseEntity<ApiResponse<Void>> selectContent(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody ContentSelectRequest request
    ) {
        roomService.selectContent(roomId, request.getContentId(), userDetails.getMember().getMemberId());
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "게임 시작", description = "게임을 시작합니다. 방장만 가능하며, 모든 참여자가 준비 완료 상태여야 합니다.")
    @PostMapping("/{roomId}/start")
    public ResponseEntity<ApiResponse<Void>> startGame(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody GameStartRequest request
    ) {
        roomService.startGame(roomId, request.getContentId(), userDetails.getMember().getMemberId());
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "영상 시청 완료", description = "영상 시청이 완료되어 역할 선택 단계로 전환합니다. 방장만 가능하며, WATCHING 단계에서만 가능합니다.")
    @PostMapping("/{roomId}/watching/finish")
    public ResponseEntity<ApiResponse<Void>> finishWatching(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        roomService.finishWatching(roomId, userDetails.getMember().getMemberId());
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "역할 확정", description = "최종 역할 할당을 확정합니다. 방장만 가능하며, ROLE_PICK 단계에서만 가능합니다.")
    @PostMapping("/{roomId}/roles/confirm")
    public ResponseEntity<ApiResponse<Void>> confirmRoles(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @Valid @RequestBody RoleConfirmRequest request,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        roomService.confirmRoles(roomId, request, userDetails.getMember().getMemberId());
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "Round 시작", description = "Round를 시작합니다. 방장만 가능하며, 역할 선택 완료 후 가능합니다. Round1 시작 시 역할 정보가 DB에 저장됩니다.")
    @PostMapping("/{roomId}/rounds/start")
    public ResponseEntity<ApiResponse<Void>> startRound(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @Valid @RequestBody RoundStartRequest request
    ) {
        roomService.startRound(roomId, request.getRound(), userDetails.getMember().getMemberId());
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "라운드 종료", description = "방장이 라운드를 강제 종료하고 부분 완료 멤버도 분석 요청합니다.")
    @PostMapping("/{roomId}/rounds/{round}/finish")
    public ResponseEntity<ApiResponse<Void>> finishRound(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @Parameter(description = "라운드 번호 (1 또는 2)") @PathVariable Integer round,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        roomService.finishRound(roomId, round, userDetails.getMember().getMemberId());
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }

    @Operation(summary = "게임 종료 (준비 단계로 복귀)", description = "게임을 종료하고 준비 단계로 복귀합니다. 방장만 가능하며, Round2 종료 시에만 사용합니다.")
    @PostMapping("/{roomId}/finish")
    public ResponseEntity<ApiResponse<Void>> finishGame(
            @Parameter(description = "방 ID") @PathVariable Long roomId,
            @AuthenticationPrincipal UserDetailsImpl userDetails
    ) {
        roomService.finishGame(roomId, userDetails.getMember().getMemberId());
        return ResponseEntity.ok(ApiResponse.successWithoutData());
    }
}
