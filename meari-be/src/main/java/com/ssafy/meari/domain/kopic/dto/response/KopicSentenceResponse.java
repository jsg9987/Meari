package com.ssafy.meari.domain.kopic.dto.response;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.ssafy.meari.domain.kopic.entity.KopicSentence;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Schema(description = "코픽 문장 응답")
public class KopicSentenceResponse {

    @Schema(description = "코픽 문장 ID", example = "10")
    private Long kopicSentenceId;

    @Schema(description = "한국어 문장", example = "안녕하세요. 아이스 아메리카노 한 잔이랑 치즈케이크 하나 주세요.")
    private String textKo;

    @Schema(description = "코픽 문장 음성 URL", example = "https://...")
    private String kopicSentenceUrl;

    @Schema(description = "코픽 참고사진 URL", example = "https://...")
    private String kopicPictureUrl;

    public static KopicSentenceResponse from(KopicSentence kopicSentence) {
        return KopicSentenceResponse.builder()
                .kopicSentenceId(kopicSentence.getKopicSentenceId())
                .textKo(kopicSentence.getTextKo())
                .kopicSentenceUrl(kopicSentence.getKopicSentenceUrl())
                .kopicPictureUrl("https://res.cloudinary.com/dznamrdwv/image/upload/kopic_picture_" + kopicSentence.getKopicSentenceId() + ".png")
                .build();
    }
}
