package com.ssafy.meari.domain.kopic.dto.response;

import com.ssafy.meari.domain.kopic.entity.KopicSentence;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("KopicSentenceResponse - 코픽 문장 응답 DTO")
class KopicSentenceResponseTest {

    @Nested
    @DisplayName("from() - KopicSentence를 Response로 변환")
    class FromTest {

        @Test
        @DisplayName("성공: KopicSentence를 KopicSentenceResponse로 변환한다")
        void from_success() {
            // Given
            KopicSentence sentence = KopicSentence.builder()
                    .kopicSentenceId(15L)
                    .textKo("안녕하세요. 아이스 아메리카노 한 잔이랑 치즈케이크 하나 주세요.")
                    .kopicSentenceUrl("https://s3.amazonaws.com/kopic/15.wav")
                    .build();

            // When
            KopicSentenceResponse response = KopicSentenceResponse.from(sentence);

            // Then
            assertThat(response.getKopicSentenceId()).isEqualTo(15L);
            assertThat(response.getTextKo()).isEqualTo("안녕하세요. 아이스 아메리카노 한 잔이랑 치즈케이크 하나 주세요.");
            assertThat(response.getKopicSentenceUrl()).isEqualTo("https://s3.amazonaws.com/kopic/15.wav");
        }

        @Test
        @DisplayName("성공: kopicPictureUrl이 올바른 형식으로 생성된다")
        void from_pictureUrl_success() {
            // Given
            KopicSentence sentence = KopicSentence.builder()
                    .kopicSentenceId(15L)
                    .textKo("테스트 문장")
                    .kopicSentenceUrl("https://s3.amazonaws.com/kopic/15.wav")
                    .build();

            // When
            KopicSentenceResponse response = KopicSentenceResponse.from(sentence);

            // Then
            assertThat(response.getKopicPictureUrl())
                    .isEqualTo("https://res.cloudinary.com/dznamrdwv/image/upload/v1770391114/kopic_picture_15.png");
        }

        @Test
        @DisplayName("성공: 다양한 sentenceId에 대해 올바른 이미지 URL을 생성한다")
        void from_pictureUrl_variousIds() {
            // Given & When & Then
            Long[] testIds = {1L, 10L, 100L, 999L, 10000L};

            for (Long id : testIds) {
                KopicSentence sentence = KopicSentence.builder()
                        .kopicSentenceId(id)
                        .textKo("테스트")
                        .kopicSentenceUrl("https://s3.amazonaws.com/kopic/" + id + ".wav")
                        .build();

                KopicSentenceResponse response = KopicSentenceResponse.from(sentence);

                assertThat(response.getKopicPictureUrl())
                        .isEqualTo("https://res.cloudinary.com/dznamrdwv/image/upload/v1770391114/kopic_picture_" + id + ".png");
            }
        }

        @Test
        @DisplayName("성공: sentenceId가 1일 때 이미지 URL을 생성한다")
        void from_pictureUrl_sentenceId1() {
            // Given
            KopicSentence sentence = KopicSentence.builder()
                    .kopicSentenceId(1L)
                    .textKo("첫 번째 문장")
                    .kopicSentenceUrl("https://s3.amazonaws.com/kopic/1.wav")
                    .build();

            // When
            KopicSentenceResponse response = KopicSentenceResponse.from(sentence);

            // Then
            assertThat(response.getKopicPictureUrl())
                    .isEqualTo("https://res.cloudinary.com/dznamrdwv/image/upload/v1770391114/kopic_picture_1.png");
        }

        @Test
        @DisplayName("성공: 모든 필드가 올바르게 매핑된다")
        void from_allFields() {
            // Given
            KopicSentence sentence = KopicSentence.builder()
                    .kopicSentenceId(42L)
                    .textKo("완벽한 테스트")
                    .kopicSentenceUrl("https://s3.amazonaws.com/kopic/42.wav")
                    .build();

            // When
            KopicSentenceResponse response = KopicSentenceResponse.from(sentence);

            // Then
            assertThat(response)
                    .extracting("kopicSentenceId", "textKo", "kopicSentenceUrl", "kopicPictureUrl")
                    .containsExactly(
                            42L,
                            "완벽한 테스트",
                            "https://s3.amazonaws.com/kopic/42.wav",
                            "https://res.cloudinary.com/dznamrdwv/image/upload/v1770391114/kopic_picture_42.png"
                    );
        }
    }
}
