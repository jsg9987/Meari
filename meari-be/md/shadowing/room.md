```
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    
    private String password;
    
    private String nickname;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    @Builder
    public User(String email, String password) {
        this.email = email;
        this.password = password;
        this.nickname = "닉네임을 설정하세요";
    }

    @Builder
    public User(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}
```


✅ 현재 계획 검토 결과

당신의 계획(Issue1~4)은 매우 적절합니다! 쉐도잉 기능의 기본 인프라(Phase 1) 구축에 정확히 필요한 것들입니다.

  ---
🔍 추가로 고려해야 할 사항

1️⃣ 비즈니스 로직 명확화 필요

Room CRUD 관련

| 상황                    | 처리 방법 (결정 필요)                                |
  |-----------------------|----------------------------------------------|
| 방장이 퇴장하면?             | ① 방 자동 삭제 / ② 다른 사람에게 방장 위임 / ③ 학습 중에는 퇴장 불가 |
| 마지막 사람이 나가면?          | ① 방 자동 삭제 (COMPLETED 상태로 변경) / ② 방 유지        |
| 학습 중(IN_PROGRESS) 입장? | ① 불가능 / ② 관전 모드 허용                           |
| 방 생성 시 owner 자동 입장?   | ① member_room에 자동 추가 / ② 별도로 /enter 호출 필요    |

역할 선점 관련

| 상황                       | 처리 방법 (결정 필요)                                   |
  |--------------------------|-------------------------------------------------|
| 이미 역할을 선점한 유저가 다른 역할 선택? | ① 기존 역할 해제 후 새 역할 선점 / ② 에러 반환                  |
| 선점되지 않은 역할?              | ① 시스템(AI)이 자동 담당 (is_system: true) / ② 학습 시작 불가 |
| 역할 해제 API 필요?            | ① 별도 DELETE API / ② 재선택으로 자동 해제                 |

  ---
2️⃣ PostgreSQL ↔ Redis 동기화 전략

[입장 시]
1. HTTP POST /api/v1/rooms/{room_id}/enter
2. PostgreSQL: member_room 레코드 INSERT
3. Redis: SADD room:{room_id}:members {member_id}
4. WebSocket: USER_ENTER 브로드캐스트

[퇴장 시]
1. HTTP DELETE /api/v1/rooms/{room_id}/leave
2. PostgreSQL: member_room 레코드 DELETE
3. Redis: SREM room:{room_id}:members {member_id}
4. Redis: HDEL room:{room_id}:roles {role_id} (역할 선점했다면)
5. WebSocket: USER_LEAVE 브로드캐스트

주의사항:
- PostgreSQL 트랜잭션 실패 시 Redis 롤백 필요
- Redis 장애 시 PostgreSQL 기준으로 복구
- 현재 참여자 수 계산: Redis SCARD 사용 (빠름) vs PostgreSQL COUNT (정확함)

  ---
3️⃣ WebSocket 연결 관리

// 구현 필요
@EventListener
public void handleWebSocketDisconnect(SessionDisconnectEvent event) {
// 1. 연결 끊긴 유저 식별
// 2. Redis 상태 정리 (member, role)
// 3. PostgreSQL member_room 삭제
// 4. USER_LEAVE 브로드캐스트
}

고려사항:
- 일시적 네트워크 끊김 vs 실제 퇴장 구분 (Grace Period)
- Heartbeat 메커니즘 필요 여부
- 재연결 시 이전 상태 복구

  ---
4️⃣ Redis 데이터 구조 설계 (추천)

# 방 참여자 목록 (Set)
room:105:members = {1, 2, 3}  # member_id들

# 역할 선점 정보 (Hash)
room:105:roles = {
"1": "3",  # role_id 1 → member_id 3이 선점
"2": "1"   # role_id 2 → member_id 1이 선점
}

# 방 상태 (String)
room:105:status = "WAITING"  # or IN_PROGRESS, COMPLETED

# 참여자별 준비 상태 (Hash) - Issue5에서 필요
room:105:ready = {
"1": "true",
"3": "false"
}

# TTL 설정 (24시간 후 자동 삭제)
EXPIRE room:105:members 86400

  ---
5️⃣ API 명세서 누락 사항

api.md에 명시되지 않은 엔드포인트들:

# 역할 선택 API (URL 미정의)
PATCH /api/v1/rooms/{room_id}/roles
Request Body: { "role_id": 1 }

# 준비 완료 API (명세 없음)
POST /api/v1/rooms/{room_id}/ready

# 방 상세 조회 (참여자 목록 포함)
GET /api/v1/rooms/{room_id}
Response: { room_id, title, members: [...], roles: [...] }

  ---
📋 전체 쉐도잉 개발 로드맵

Phase 1: 기본 인프라 ✅ (Issue1~4, 당신의 현재 계획)

✅ Issue1: Room CRUD
✅ Issue2: Redis + WebSocket 설정
✅ Issue3: 입장/퇴장 권한 및 상태 동기화
✅ Issue4: 역할 선점 시스템

Phase 2: 학습 준비

Issue5: 참여자 Ready 시스템
- POST /api/v1/rooms/{room_id}/ready (토글)
- Redis에 준비 상태 저장
- WebSocket으로 USER_READY 브로드캐스트

Issue6: 게임 시작 (방장 권한)
- POST /api/v1/rooms/{room_id}/start
- 검증: 모든 역할 배정 완료? 모두 Ready?
- RoomStatus: WAITING → IN_PROGRESS
- Content, Sentence 정보 조회하여 반환

Phase 3: 실시간 학습 진행

Issue7: 영상 동기화 (WebSocket)
- Send: /app/room/{room_id}/video-sync
- Subscribe: /topic/room/{room_id}/video-sync
- Payload: { action: "PLAY/PAUSE", current_time: 12.5 }

Issue8: 턴 알림 시스템
- Sentence의 start_time 기준으로 서버가 턴 계산
- Subscribe: /topic/room/{room_id}/turn
- Payload: { sentence_id, role_id, member_id, start_time, end_time }

Issue9: 실시간 채팅
- Send: /app/room/{room_id}/chat
- Subscribe: /topic/room/{room_id}/chat

Phase 4: 녹음 및 분석

Issue10: S3 Presigned URL 발급
- POST /api/v1/s3/presigned-url
- Request: { file_name, content_type }
- Response: { presigned_url, audio_url }

Issue11: 쉐도잉 리포트 생성
- POST /api/v1/shadowing/analyze
- Request: { sentence_id, audio_url }
- shadowing_report INSERT (status: PROCESSING)

Issue12: AI 분석 연동
- RabbitMQ 또는 HTTP로 FastAPI 호출
- 콜백으로 결과 수신
- shadowing_report UPDATE (status: COMPLETED, JSONB 저장)

Issue13: 리포트 조회
- GET /api/v1/shadowing/reports/{shadowing_report_id}

Phase 5: 세션 종료

Issue14: 학습 종료 처리
- POST /api/v1/rooms/{room_id}/finish
- RoomStatus: IN_PROGRESS → COMPLETED
- Redis 세션 정리 (또는 TTL로 자동 만료)
- 종합 리포트 생성

Phase 6: 고급 기능

Issue15: WebRTC 시그널링
- /app/room/{room_id}/signal
- SDP, ICE Candidate 교환

Issue16: 재접속 처리
- WebSocket 재연결 시 이전 상태 복구
- Redis에서 참여자 정보 조회

Issue17: 에러 복구
- 타임아웃 처리
- 장애 상황 대응

  ---
🎯 Issue1~4 구현 시 주의사항

1. Issue1: Room CRUD

// RoomService.java
public RoomResponse createRoom(RoomCreateRequest request, Long memberId) {
// 1. Content 존재 여부 확인
Content content = contentRepository.findById(request.getContentId())
.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CONTENT));

      // 2. Room 생성 (초기 상태: WAITING)
      Room room = Room.builder()
          .owner(memberRepository.getReferenceById(memberId))
          .content(content)
          .title(request.getTitle())
          .password(request.getPassword())
          .isActive(RoomStatus.WAITING)
          .build();

      roomRepository.save(room);

      // 3. (선택) 방장 자동 입장
      MemberRoom memberRoom = MemberRoom.builder()
          .member(room.getOwner())
          .room(room)
          .build();
      memberRoomRepository.save(memberRoom);

      return RoomResponse.from(room);
}

2. Issue2: Redis + WebSocket 설정

// WebSocketConfig.java
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

      @Override
      public void configureMessageBroker(MessageBrokerRegistry config) {
          config.enableSimpleBroker("/topic");  // 구독 prefix
          config.setApplicationDestinationPrefixes("/app");  // 발행 prefix
      }

      @Override
      public void registerStompEndpoints(StompEndpointRegistry registry) {
          registry.addEndpoint("/ws")
              .setAllowedOrigins("http://localhost:5173")
              .withSockJS();
      }
}

3. Issue3: 입장 권한 확인

// RoomService.java
@Transactional
public void enterRoom(Long roomId, String password, Long memberId) {
Room room = roomRepository.findById(roomId)
.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_RESOURCE));

      // 비밀번호 확인
      if (room.getPassword() != null && !room.getPassword().equals(password)) {
          throw new BusinessException(ErrorCode.INVALID_PASSWORD); // 403
      }

      // 정원 확인
      long currentCount = memberRoomRepository.countByRoomId(roomId);
      if (currentCount >= room.getContent().getMaxPeople()) {
          throw new BusinessException(ErrorCode.ROOM_FULL); // 409
      }

      // 중복 입장 체크
      boolean alreadyJoined = memberRoomRepository.existsByRoomIdAndMemberId(roomId, memberId);
      if (alreadyJoined) {
          throw new BusinessException(ErrorCode.ALREADY_JOINED); // 409
      }

      // PostgreSQL 저장
      MemberRoom memberRoom = MemberRoom.builder()
          .room(room)
          .member(memberRepository.getReferenceById(memberId))
          .build();
      memberRoomRepository.save(memberRoom);

      // Redis 저장
      redisTemplate.opsForSet().add("room:" + roomId + ":members", memberId.toString());

      // WebSocket 브로드캐스트
      RoomEvent event = RoomEvent.builder()
          .type("USER_ENTER")
          .memberId(memberId)
          .roomId(roomId)
          .build();
      messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", event);
}

4. Issue4: 역할 선점

// RoomService.java
public void assignRole(Long roomId, Long roleId, Long memberId) {
// 1. 역할이 해당 방의 content에 속하는지 확인
Role role = roleRepository.findById(roleId)
.orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_RESOURCE));

      Room room = roomRepository.findById(roomId)
          .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_RESOURCE));

      if (!role.getContent().getContentId().equals(room.getContent().getContentId())) {
          throw new BusinessException(ErrorCode.INVALID_ARGUMENT);
      }

      // 2. Redis HSETNX로 원자성 보장
      String key = "room:" + roomId + ":roles";
      Boolean success = redisTemplate.opsForHash().putIfAbsent(
          key,
          roleId.toString(),
          memberId.toString()
      );

      if (!success) {
          throw new BusinessException(ErrorCode.ROLE_ALREADY_TAKEN); // 409
      }

      // 3. WebSocket 브로드캐스트
      RoleEvent event = RoleEvent.builder()
          .type("ROLE_ASSIGNED")
          .roomId(roomId)
          .roleId(roleId)
          .memberId(memberId)
          .build();
      messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", event);
}

  ---
✅ 최종 평가

당신의 계획이 올바른 이유:

1. ✅ 순차적 구현: CRUD → 실시간 통신 → 상태 관리 순서가 적절
2. ✅ 기술 스택 적합: Redis + WebSocket 조합은 실시간 세션 관리에 최적
3. ✅ 확장 가능: Phase 1 완료 후 Phase 2~6 진행 가능

추가로 결정해야 할 것:

1. ❓ 비즈니스 로직 (방장 퇴장, 역할 중복 등)
2. ❓ 에러 코드 추가 (ErrorCode enum에 ROOM_FULL, ROLE_ALREADY_TAKEN 등)
3. ❓ API URL 확정 (역할 선택 엔드포인트 등)

  ---
다음 단계: Issue1 CRUD 구현 준비

다음 대화에서 구현할 내용:
1. RoomController - 방 생성, 조회, 입장, 퇴장 API
2. RoomService - 비즈니스 로직
3. RoomRepository - JPA 쿼리 메서드
4. DTO 클래스 - Request/Response
5. ErrorCode 추가 - ROOM 관련 에러 코드

준비 되셨다면 시작하겠습니다! 👍