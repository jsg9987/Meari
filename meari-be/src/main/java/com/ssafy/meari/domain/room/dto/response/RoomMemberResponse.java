package com.ssafy.meari.domain.room.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.member.entity.Member;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Schema(description = "방 참여자 정보")
@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RoomMemberResponse {

    @Schema(description = "회원 ID", example = "1")
    private Long memberId;

    @Schema(description = "닉네임", example = "김철수")
    private String nickname;

    @Schema(description = "프로필 이미지 URL")
    private String profileUrl;

    @Schema(description = "방장 여부", example = "true")
    private Boolean isOwner;

    @Schema(description = "준비 완료 여부", example = "false")
    private Boolean isReady;

    @Schema(description = "선점한 역할 ID (없으면 null)", example = "1")
    private Long roleId;

    public static RoomMemberResponse from(Member member, boolean isOwner, boolean isReady, Long roleId) {
        return RoomMemberResponse.builder()
                .memberId(member.getMemberId())
                .nickname(member.getNickname())
                .profileUrl(member.getProfileUrl())
                .isOwner(isOwner)
                .isReady(isReady)
                .roleId(roleId)
                .build();
    }
}
