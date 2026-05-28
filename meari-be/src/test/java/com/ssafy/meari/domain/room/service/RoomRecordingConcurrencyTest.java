package com.ssafy.meari.domain.room.service;

import com.ssafy.meari.domain.analysis.service.AnalysisService;
import com.ssafy.meari.domain.content.repository.ContentRepository;
import com.ssafy.meari.domain.content.repository.RoleRepository;
import com.ssafy.meari.domain.content.repository.SentenceRepository;
import com.ssafy.meari.domain.member.repository.MemberRepository;
import com.ssafy.meari.domain.report.repository.ShadowingReportRepository;
import com.ssafy.meari.domain.room.dto.websocket.RecordingCompleteMessage;
import com.ssafy.meari.domain.room.entity.GamePhase;
import com.ssafy.meari.domain.room.repository.MemberRoomRepository;
import com.ssafy.meari.domain.room.repository.RoomRepository;
import com.ssafy.meari.domain.theme.repository.ThemeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.test.util.ReflectionTestUtils;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 녹음 완료 → 분석 요청 동시성 테스트.
 *
 * 배경: recordingComplete()는 "모든 문장 완료?(isMemberRecordingsComplete)" 체크 후
 * "분석 요청(requestMemberAnalysis)"을 호출하는데, 이 check-then-act가 비원자적이다.
 * recordings는 Redis Set이라 마지막 문장이 중복 도착(두 탭/콘솔/재전송)하면
 * 모든 중복 메시지가 "size==total 완료"를 보고 각자 분석을 요청 → 비싼 Wav2Vec2 추론이 N배 낭비.
 *
 * 실제 Redis(Testcontainers)로 SETNX 원자성까지 검증한다.
 * - 수정 전: 마지막 문장이 동시에 N개 도착하면 분석 요청이 N회 발생 (RED)
 * - 수정 후: 정확히 1회만 발생 (GREEN)
 */
@Testcontainers
@DisplayName("녹음 완료 → 분석 요청 동시성 테스트 (실제 Redis)")
class RoomRecordingConcurrencyTest {

    @Container
    static final GenericContainer<?> REDIS =
            new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
                    .withExposedPorts(6379);

    private static final Long ROOM_ID = 1L;
    private static final Long MEMBER_ID = 100L;
    private static final int ROUND = 1;
    private static final int TOTAL_SENTENCES = 5;
    private static final Long LAST_SENTENCE_ID = 5L;

    private LettuceConnectionFactory connectionFactory;
    private RoomSessionService roomSessionService;
    private RoomService roomService;
    private AnalysisService analysisService; // mock — 호출 횟수 카운트용

    @BeforeEach
    void setUp() {
        // 실제 Redis 연결 (Testcontainers가 띄운 컨테이너)
        connectionFactory = new LettuceConnectionFactory(REDIS.getHost(), REDIS.getMappedPort(6379));
        connectionFactory.afterPropertiesSet();

        RedisTemplate<String, String> redisTemplate = new RedisTemplate<>();
        redisTemplate.setConnectionFactory(connectionFactory);
        StringRedisSerializer string = new StringRedisSerializer();
        redisTemplate.setKeySerializer(string);
        redisTemplate.setValueSerializer(string);
        redisTemplate.setHashKeySerializer(string);
        redisTemplate.setHashValueSerializer(string);
        redisTemplate.afterPropertiesSet();

        // 테스트 간 격리: 매번 초기화
        connectionFactory.getConnection().serverCommands().flushAll();

        roomSessionService = new RoomSessionService(redisTemplate);
        analysisService = mock(AnalysisService.class);

        // recordingComplete()는 DB를 타지 않고 Redis만 사용하므로 Repository류는 mock으로 충분
        roomService = new RoomService(
                mock(RoomRepository.class),
                mock(MemberRoomRepository.class),
                mock(MemberRepository.class),
                mock(ThemeRepository.class),
                mock(ContentRepository.class),
                mock(RoleRepository.class),
                mock(SentenceRepository.class),
                mock(ShadowingReportRepository.class),
                roomSessionService,
                mock(RoomBroadcastService.class),
                analysisService,
                mock(RoomQueryService.class)
        );
        ReflectionTestUtils.setField(roomService, "s3Bucket", "test-bucket");

        // 초기 상태: ROUND_1 진행 중, 멤버 1명, 총 5문장, 1~4번 이미 녹음 완료 (마지막 5번만 남음)
        roomSessionService.setPhase(ROOM_ID, GamePhase.ROUND_1);
        roomSessionService.addMember(ROOM_ID, MEMBER_ID);
        roomSessionService.setMemberTotalSentences(ROOM_ID, ROUND, MEMBER_ID, TOTAL_SENTENCES);
        for (long s = 1; s < LAST_SENTENCE_ID; s++) {
            roomSessionService.markRecordingComplete(ROOM_ID, ROUND, MEMBER_ID, s);
        }
    }

    @AfterEach
    void tearDown() {
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
    }

    @Test
    @DisplayName("마지막 문장 녹음완료가 동시에 N개 도착해도 분석 요청은 1회만 발생한다")
    void analysisRequestedExactlyOnce_underConcurrentDuplicateMessages() throws InterruptedException {
        int threadCount = 10;
        ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        CountDownLatch ready = new CountDownLatch(threadCount);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);

        for (int i = 0; i < threadCount; i++) {
            pool.submit(() -> {
                RecordingCompleteMessage message = new RecordingCompleteMessage();
                message.setMemberId(MEMBER_ID);
                message.setSentenceId(LAST_SENTENCE_ID);
                message.setAudioUrl(null); // S3 변환 분기 회피 (동시성 검증에 불필요)

                ready.countDown();
                try {
                    start.await();                 // 모든 스레드가 동시에 출발하도록 정렬
                    roomService.recordingComplete(ROOM_ID, message);
                } catch (Exception ignored) {
                    // 본 테스트의 관심사는 분석 요청 횟수
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await();          // 전 스레드 준비 완료까지 대기
        start.countDown();      // 동시 발사
        done.await(10, TimeUnit.SECONDS);
        pool.shutdownNow();

        // 핵심 단언: 마지막 문장이 10번 동시 도착해도 분석 요청은 정확히 1회
        // (수정 전에는 10회 호출되어 실패 → race 재현 증거)
        verify(analysisService, times(1)).requestMemberAnalysis(ROOM_ID, ROUND, MEMBER_ID);
    }
}
