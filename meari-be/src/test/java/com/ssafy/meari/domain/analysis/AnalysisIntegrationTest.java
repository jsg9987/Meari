package com.ssafy.meari.domain.analysis;

import com.ssafy.meari.domain.analysis.service.AnalysisService;
import com.ssafy.meari.domain.content.entity.Content;
import com.ssafy.meari.domain.content.entity.Role;
import com.ssafy.meari.domain.content.entity.Sentence;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.content.repository.SentenceRepository;
import com.ssafy.meari.domain.member.entity.Member;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.report.entity.ShadowingReport;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.domain.room.entity.Room;
import com.ssafy.meari.domain.room.entity.RoomStatus;
import com.ssafy.meari.domain.room.repository.RoomRepository;
import com.ssafy.meari.domain.room.service.RoomSessionService;
import com.ssafy.meari.domain.theme.entity.Theme;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * 발음 분석 통합 테스트
 * Spring Boot → FastAPI 전체 플로우 검증
 */
@Slf4j
@SpringBootTest
@ActiveProfiles("test")
class AnalysisIntegrationTest {

    @Autowired
    private AnalysisService analysisService;

    @Autowired
    private RoomSessionService roomSessionService;

    @Autowired
    private MemberRepository memberRepository;

    @Autowired
    private ThemeRepository themeRepository;

    @Autowired
    private ContentRepository contentRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private SentenceRepository sentenceRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private ShadowingReportRepository shadowingReportRepository;

    @Autowired
    private RedisTemplate<String, String> redisTemplate;

    private Long roomId;
    private Long memberAId;
    private Long memberBId;
    private Long roleAId;
    private Long roleBId;
    private Long[] sentenceAIds = new Long[4];
    private Long[] sentenceBIds = new Long[4];

    @BeforeEach
    void setUp() {
        log.info("=== 테스트 데이터 준비 시작 ===");

        // 1. 회원 생성
        Member memberA = memberRepository.save(Member.builder()
                .email("testA@test.com")
                .password("password")
                .nickname("테스트A")
                .build());
        memberAId = memberA.getMemberId();

        Member memberB = memberRepository.save(Member.builder()
                .email("testB@test.com")
                .password("password")
                .nickname("테스트B")
                .build());
        memberBId = memberB.getMemberId();

        log.info("회원 생성 완료: memberA={}, memberB={}", memberAId, memberBId);

        // 2. 테마 및 콘텐츠 생성
        Theme theme = themeRepository.save(Theme.builder()
                .name("테스트 테마")
                .description("테스트용 테마입니다")
                .themeUrl("https://test.com/theme.jpg")
                .build());

        Content content = contentRepository.save(Content.builder()
                .theme(theme)
                .title("테스트 콘텐츠")
                .videoUrl("https://test.com/video.mp4")
                .maxPeople(2)
                .totalDuration(BigDecimal.valueOf(120))
                .build());

        log.info("콘텐츠 생성 완료: contentId={}", content.getContentId());

        // 3. 역할 생성
        Role roleA = roleRepository.save(Role.builder()
                .name("역할A")
                .content(content)
                .build());
        roleAId = roleA.getRoleId();

        Role roleB = roleRepository.save(Role.builder()
                .name("역할B")
                .content(content)
                .build());
        roleBId = roleB.getRoleId();

        log.info("역할 생성 완료: roleA={}, roleB={}", roleAId, roleBId);

        // 4. 문장 생성 (각 역할당 4개)
        for (int i = 0; i < 4; i++) {
            Sentence sentenceA = sentenceRepository.save(Sentence.builder()
                    .content(content)
                    .role(roleA)
                    .sequence(i + 1)
                    .textKo("테스트 문장 A-" + (i + 1))
                    .textVn("Test sentence A-" + (i + 1))
                    .startTime(BigDecimal.valueOf(i * 10))
                    .endTime(BigDecimal.valueOf((i + 1) * 10))
                    .referenceAudioKey("reference/roleA/sentence" + (i + 1) + ".wav")
                    .build());
            sentenceAIds[i] = sentenceA.getSentenceId();

            Sentence sentenceB = sentenceRepository.save(Sentence.builder()
                    .content(content)
                    .role(roleB)
                    .sequence(i + 1)
                    .textKo("테스트 문장 B-" + (i + 1))
                    .textVn("Test sentence B-" + (i + 1))
                    .startTime(BigDecimal.valueOf(i * 10 + 5))
                    .endTime(BigDecimal.valueOf((i + 1) * 10 + 5))
                    .referenceAudioKey("reference/roleB/sentence" + (i + 1) + ".wav")
                    .build());
            sentenceBIds[i] = sentenceB.getSentenceId();
        }

        log.info("문장 생성 완료: roleA 4개, roleB 4개");

        // 5. 방 생성
        Room room = Room.builder()
                .owner(memberA)
                .theme(theme)
                .title("테스트 방")
                .maxPeople(4)
                .build();
        room.updateStatus(RoomStatus.IN_PROGRESS);
        room = roomRepository.save(room);
        roomId = room.getRoomId();

        log.info("방 생성 완료: roomId={}", roomId);

        // 6. ShadowingReport 생성 (2명)
        shadowingReportRepository.save(ShadowingReport.builder()
                .member(memberA)
                .room(room)
                .role(roleA)
                .content(content)
                .round(1)
                .build());

        shadowingReportRepository.save(ShadowingReport.builder()
                .member(memberB)
                .room(room)
                .role(roleB)
                .content(content)
                .round(1)
                .build());

        log.info("ShadowingReport 생성 완료");

        // 7. Redis 세션 설정
        roomSessionService.addMember(roomId, memberAId);
        roomSessionService.addMember(roomId, memberBId);
        roomSessionService.setMemberTotalSentences(roomId, 1, memberAId, 4);
        roomSessionService.setMemberTotalSentences(roomId, 1, memberBId, 4);

        log.info("Redis 세션 설정 완료");
        log.info("=== 테스트 데이터 준비 완료 ===\n");
    }

    @AfterEach
    void tearDown() {
        log.info("=== 테스트 데이터 정리 시작 ===");

        // 1. Redis 데이터 삭제
        if (roomId != null) {
            redisTemplate.delete("room:" + roomId + ":session");
            redisTemplate.delete("room:" + roomId + ":round:1:member:" + memberAId);
            redisTemplate.delete("room:" + roomId + ":round:1:member:" + memberBId);
        }

        // 2. ShadowingReport 삭제
        shadowingReportRepository.deleteAll();

        // 3. Sentence 삭제
        sentenceRepository.deleteAll();

        // 4. Role 삭제
        roleRepository.deleteAll();

        // 5. Room 삭제
        roomRepository.deleteAll();

        // 6. Content 삭제
        contentRepository.deleteAll();

        // 7. Theme 삭제
        themeRepository.deleteAll();

        // 8. Member 삭제
        memberRepository.deleteAll();

        log.info("=== 테스트 데이터 정리 완료 ===\n");
    }

    @Test
    @DisplayName("2명이 각각 4문장씩 녹음 완료 → FastAPI 분석 요청")
    void testFullAnalysisFlow() throws InterruptedException {
        log.info("\n=== 테스트 시작: 2명 × 4문장 분석 플로우 ===\n");

        int round = 1;

        // Member A: 4문장 녹음 완료
        log.info("--- Member A 녹음 시작 ---");
        for (int i = 0; i < 4; i++) {
            String audioUrl = String.format("s3://meari-bucket/test/memberA/sentence%d.wav", i + 1);

            // Redis에 녹음 완료 마킹
            roomSessionService.markRecordingComplete(roomId, round, memberAId, sentenceAIds[i]);
            roomSessionService.saveAudioUrl(roomId, round, memberAId, sentenceAIds[i], audioUrl);

            log.info("Member A - 문장 {} 녹음 완료: {}", i + 1, audioUrl);
        }

        // Member A 모든 녹음 완료 확인
        boolean memberAComplete = roomSessionService.isMemberRecordingsComplete(roomId, round, memberAId);
        assertThat(memberAComplete).isTrue();
        log.info("✅ Member A 모든 녹음 완료 확인");

        // Member B: 4문장 녹음 완료
        log.info("\n--- Member B 녹음 시작 ---");
        for (int i = 0; i < 4; i++) {
            String audioUrl = String.format("s3://meari-bucket/test/memberB/sentence%d.wav", i + 1);

            // Redis에 녹음 완료 마킹
            roomSessionService.markRecordingComplete(roomId, round, memberBId, sentenceBIds[i]);
            roomSessionService.saveAudioUrl(roomId, round, memberBId, sentenceBIds[i], audioUrl);

            log.info("Member B - 문장 {} 녹음 완료: {}", i + 1, audioUrl);
        }

        // Member B 모든 녹음 완료 확인
        boolean memberBComplete = roomSessionService.isMemberRecordingsComplete(roomId, round, memberBId);
        assertThat(memberBComplete).isTrue();
        log.info("✅ Member B 모든 녹음 완료 확인");

        // 분석 요청 (Member A)
        log.info("\n--- Member A 분석 요청 ---");
        analysisService.requestMemberAnalysis(roomId, round, memberAId);
        log.info("✅ Member A 분석 요청 완료 (비동기 실행 중)");

        // 분석 요청 (Member B)
        log.info("\n--- Member B 분석 요청 ---");
        analysisService.requestMemberAnalysis(roomId, round, memberBId);
        log.info("✅ Member B 분석 요청 완료 (비동기 실행 중)");

        // 비동기 처리 대기 (실제 FastAPI 호출 시간)
        log.info("\n⏳ 분석 결과 대기 중... (최대 120초)");
        Thread.sleep(120000); // 2분 대기

        // 결과 확인
        log.info("\n--- 분석 결과 확인 ---");
        ShadowingReport reportA = shadowingReportRepository
                .findByRoom_RoomIdAndRoundAndMember_MemberId(roomId, round, memberAId)
                .orElseThrow();

        ShadowingReport reportB = shadowingReportRepository
                .findByRoom_RoomIdAndRoundAndMember_MemberId(roomId, round, memberBId)
                .orElseThrow();

        log.info("Member A 결과: accuracy={}, intonation={}",
                reportA.getAccuracy(), reportA.getIntonation());
        log.info("Member B 결과: accuracy={}, intonation={}",
                reportB.getAccuracy(), reportB.getIntonation());

        // 검증
        assertNotNull(reportA.getAccuracy(), "Member A accuracy가 null이면 안됨");
        assertNotNull(reportB.getAccuracy(), "Member B accuracy가 null이면 안됨");

        log.info("\n=== 테스트 완료 ===");
    }

    @Test
    @DisplayName("Member A만 1문장 녹음 → 분석 요청 실패 (모든 문장 완료 안됨)")
    void testPartialRecording() {
        log.info("\n=== 테스트: 부분 녹음 (분석 요청 안됨) ===\n");

        int round = 1;
        String audioUrl = "s3://meari-bucket/test/memberA/sentence1.wav";

        // Member A: 1문장만 녹음
        roomSessionService.markRecordingComplete(roomId, round, memberAId, sentenceAIds[0]);
        roomSessionService.saveAudioUrl(roomId, round, memberAId, sentenceAIds[0], audioUrl);

        // 완료 확인
        boolean isComplete = roomSessionService.isMemberRecordingsComplete(roomId, round, memberAId);

        assertThat(isComplete).isFalse();
        log.info("✅ 1개 문장만 완료 → 분석 요청 안됨 (정상)");
    }
}
