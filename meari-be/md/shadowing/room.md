# 쉐도잉 기능 통합 설계 및 작업 내역

## 1. 개요

4인 준실시간 한국어 쉐도잉 학습 방 기능 구현

### 1.1. 핵심 플로우 (2026-01-28 최종)

```
1. 방 생성 (WAITING)
   ↓
2. 동영상 선택
   ↓
3. 참여자 준비
   ↓
4. 게임 시작 (IN_PROGRESS + WATCHING) // 여기서부터 입장 불가
   ↓
5. 영상 시청
   ↓
6. 영상 시청 완료 (ROLE_PICK)
   ↓
7. 역할 선택
   ↓
8. 역할 확정
   ↓
9. Round1 시작 (ROUND_1)
   - 방장은 /rounds/start API 호출
   - 서버는 멤버별 문장 목록(segments)을 담아 ROUND_START 메시지 브로드캐스팅
   ↓
10. Round1 진행
    - 클라이언트는 자기 턴에 문장 녹음 후 S3 업로드
    - 업로드 완료 후 /recording/complete 메시지 전송
    - 서버는 모든 멤버의 녹음 완료 시 RECORDINGS_COMPLETE 메시지 브로드캐스팅
   ↓
11. Round2 시작 (ROUND_2)
    - 방장은 /rounds/start API 호출
    - (이하 Round1과 동일)
   ↓
12. Round2 진행
   ↓
13. 게임 종료 (WAITING으로 복귀)
   ↓
다시 2번부터 반복 가능
```

### 1.2. RoomStatus 상태 관리
| 상태 | 설명 | 입장 가능 |
|------|------|----------|
| WAITING | 대기 중 (준비 단계) | O |
| IN_PROGRESS | 진행 중 (시작~학습 종료) | X |
| COMPLETED | 종료됨 (현재는 사용 안함, 방 삭제로 대체) | X |


### 1.3. GamePhase 상태 전환 다이어그램 (Redis 관리)
```
WAITING (방 대기)
  ↓ startGame
WATCHING (영상 시청)
  ↓ finishWatching
ROLE_PICK (역할 선택)
  ↓ startRound(1)
ROUND_1 (라운드 1)
  ↓ startRound(2)
ROUND_2 (라운드 2)
  ↓ finishGame
WAITING (다시 대기)
```
- `room:{room_id}:phase` 키에 저장

---

## 2. API 설계 (2026-01-28 최종)

### 2.1. HTTP REST API

| Method | URL | 설명 | 권한 | 상태 조건 |
|--------|-----|------|------|-----------|
| POST | /api/v1/rooms | 방 생성 | 인증된 사용자 | - |
| GET | /api/v1/rooms | 방 목록 조회 (커서 기반) | 인증된 사용자 | - |
| GET | /api/v1/rooms/{roomId} | 방 상세 조회 | 인증된 사용자 | - |
| POST | /api/v1/rooms/{roomId}/enter | 방 입장 | 인증된 사용자 | WAITING |
| DELETE | /api/v1/rooms/{roomId}/leave | 방 퇴장 | 방 참여자 | - |
| POST | /api/v1/rooms/{roomId}/content | 동영상 선택 | 방장 | WAITING |
| POST | /api/v1/rooms/{roomId}/ready | 준비 상태 토글 | 방 참여자 | WAITING |
| POST | /api/v1/rooms/{roomId}/start | 게임 시작 | 방장 | WAITING |
| POST | /api/v1/rooms/{roomId}/watching/finish | 영상 시청 완료 | 방장 | WATCHING |
| POST | /api/v1/rooms/{roomId}/roles/confirm | 역할 확정 | 방장 | ROLE_PICK |
| POST | /api/v1/rooms/{roomId}/rounds/start | Round 시작. ROUND_START 메시지 브로드캐스팅 트리거 | 방장 | ROLE_PICK/ROUND_1 |
| POST | /api/v1/rooms/{roomId}/finish | 게임 종료 (준비로 복귀)| 방장 | ROUND_2 |
| POST | /api/v1/rooms/{roomId}/role | 역할 선점 | 방 참여자 | ROLE_PICK |
| POST | /api/v1/s3/presigned-url | S3 업로드 URL 요청 | 인증된 사용자 | - |
| POST | /api/v1/shadowing/analyze | 분석 요청 | 인증된 사용자 | - |
| GET | /api/v1/shadowing/reports/{id} | 리포트 조회 | 인증된 사용자 | - |


### 2.2. WebSocket API

#### JWT 인증
- STOMP `CONNECT` 프레임의 `Authorization` 헤더를 `JwtChannelInterceptor`에서 검증.
- 인증 정보는 WebSocket 세션에 저장.

#### Server-bound (Client -> Server)
| Destination | 설명 | 페이로드 예시 |
|-------------|------|---------------|
| /app/room/{roomId}/ready | 준비/준비해제 | `{ "memberId": 1 }` |
| /app/room/{roomId}/role/assign | 역할 선점 | `{ "memberId": 1, "roleId": 2 }` |
| /app/room/{roomId}/role/release | 역할 해제 | `{ "memberId": 1 }` |
| /app/room/{roomId}/chat | 채팅 메시지 전송 | `{ "memberId": 1, "content": "hello" }` |
| /app/room/{roomId}/recording/complete | 문장 녹음 완료 | `{ "memberId": 1, "sentenceId": 123, "audioUrl": "..." }` |

#### Client-bound (Server -> Client)
| Topic | 설명 | 페이로드 타입 / 예시 |
|-------|------|----------|
| /topic/room/{roomId}/state | 참여자 변경, 준비 상태, 역할 선점, **라운드 시작/종료** 등 방의 전반적인 상태 변경 | `MEMBER_JOIN`, `READY`, `ROLE_ASSIGNED`, `PHASE_CHANGE`, `ROUND_START`, `RECORDINGS_COMPLETE` |
| /topic/room/{roomId}/video-sync | 영상 동기화 (재생, 정지, 시간 이동) | `{ "action": "PLAY", "currentTime": 15.2 }` |
| /topic/room/{roomId}/turn | 현재 발화해야 할 쉐도잉 턴 알림 | `{ "sentenceId": 123, "memberId": 45 }` |
| /topic/room/{roomId}/chat | 인게임 채팅 | `{ "sender": "nickname", "message": "hello" }` |


---

## 3. 비즈니스 로직

### 3.1. 퇴장/연결 끊김 처리 (Grace Period 적용)
- **일반 퇴장**: `leaveRoom` API 호출 시 즉시 처리.
- **연결 끊김**:
    1. WebSocket `SessionDisconnectEvent` 발생.
    2. Redis에 `room:{roomId}:disconnected` 해시값으로 `memberId: timestamp` 저장.
    3. 30초의 Grace Period 시작 (ScheduledExecutorService 사용).
    4. 30초 내 재연결 시(WebSocket 메시지 수신 감지) `disconnected` 마킹 해제.
    5. 30초 초과 시 스케줄된 `leaveRoom` 로직 자동 실행.
- **방장 위임**: WAITING 또는 IN_PROGRESS 상태에서 방장 퇴장 시, 가장 먼저 입장한 사람에게 방장 자동 위임.
- **방 자동 삭제**: 마지막 1명이 퇴장하면 방 자동 삭제.

### 3.2. 역할 선점
- Redis `HSETNX` 명령어로 원자성 보장 (`room:{roomId}:roles`).
- 이미 역할을 선점한 유저가 다른 역할을 선택하면 기존 역할 해제 후 새 역할 선점.
- 선점되지 않은 역할은 시스템(AI)이 자동 담당.

### 3.3. 라운드 진행 및 녹음
- **라운드 시작**: `startRound` 시, DB에서 `Sentence` 목록을 조회하고 역할에 맞게 분배하여 `MemberSegmentInfo` 리스트를 구성. 이를 `ROUND_START` 메시지에 담아 브로드캐스팅.
- **녹음 완료**: 클라이언트가 `/recording/complete` 메시지 전송. 서버는 Redis Set에 녹음 완료된 문장 ID를 저장.
- **전체 녹음 완료 감지**: 한 멤버의 녹음 완료 메시지 수신 시, 모든 멤버가 자신의 모든 문장을 녹음했는지 `isAllRecordingsComplete`를 통해 확인. 모두 완료 시 `RECORDINGS_COMPLETE` 메시지 브로드캐스팅.

---

## 4. Redis 데이터 구조 (최종)
```redis
# 방 참여자 목록 (Set)
room:{roomId}:members = Set<memberId>

# 역할 선점 정보 (Hash: roleId -> memberId | "SYSTEM")
room:{roomId}:roles = Hash { roleId -> memberId }

# 참여자별 준비 상태 (Hash: memberId -> "true")
room:{roomId}:ready = Hash { memberId -> "true" }

# 역할 확정 플래그 (String)
room:{roomId}:roles_confirmed = "true"

# 현재 선택된 콘텐츠 ID (String)
room:{roomId}:content_id = contentId

# 현재 게임 진행 단계 (String: WATCHING | ROLE_PICK | ROUND_1 | ROUND_2)
room:{roomId}:phase = "WATCHING"

# 연결 끊김 추적 (Hash: memberId -> timestamp)
room:{roomId}:disconnected = Hash { memberId -> timestamp }

# 멤버 ID로 방 ID를 찾기 위한 매핑 (String) - Grace Period 자동 퇴장 시 사용
member:{memberId}:roomId = roomId

# --- 라운드 진행 관련 ---

# 라운드 시작 시간 (String: epoch millis)
room:{roomId}:round_start_time = 1675132800000

# 멤버별 녹음 완료 문장 ID 목록 (Set)
room:{roomId}:round:{round}:member:{memberId}:recordings = Set<sentenceId>

# 멤버별 할당된 총 문장 수 (String)
room:{roomId}:round:{round}:member:{memberId}:total_sentences = 5


# 모든 방 관련 키는 24시간 후 자동 만료 (EXPIRE)
```

---

## 5. 분석 처리 플로우
```
1. 클라이언트: 문장 발화 후 S3 Presigned URL로 음성 파일 업로드.
2. 클라이언트: S3 업로드 완료 후, 백엔드에 녹음 완료 메시지 전송 (`/app/room/{roomId}/recording/complete` with audio_url).
3. 백엔드: 모든 참여자 녹음 완료 시, 리포트 생성 및 AI 분석 요청을 위한 로직 실행 준비.
...
```

### 5.1. 분석 결과 JSON 구조 예시
```json
{
  "summary": { "avg_accuracy": 82, "avg_intonation": 87 },
  "sentences": [
    {
      "sentence_id": 11,
      "text_ko": "어서오세요",
      "accuracy": 85,
      "intonation": 90,
      "accuracy_detail": { ... },
      "intonation_detail": { ... }
    }
  ]
}
```

---

## 6. 전체 개발 로드맵 및 현황

- ✅: 완료
- 🔄: 진행 중
- ◻️: 예정

### Phase 1: 기본 인프라
- ✅ Room CRUD (생성, 조회, 입장, 퇴장)
- ✅ Redis + WebSocket 기본 설정
- ✅ 입장/퇴장 권한 및 상태 동기화
- ✅ 역할 선점 시스템 (`/role` API, `HSETNX` 로직)
- ✅ WebSocket JWT 인증 구현 (`JwtChannelInterceptor`)

### Phase 2: 학습 준비
- ✅ 참여자 Ready 시스템 (`/ready` API, 상태 브로드캐스팅)
- ✅ 게임 시작 기능 (`/start` API, 상태 검증, `IN_PROGRESS`로 변경)
- ✅ 동영상 선택 기능 (`/content` API)
- ✅ 영상 시청 완료 기능 (`/watching/finish` API)

### Phase 3: 실시간 학습 진행
- ✅ **라운드 시작 및 문장 분배 로직**
- ✅ **라운드별 녹음 완료 상태 관리 및 감지**
- ◻️ Issue7: 영상 동기화 (WebSocket `/video-sync`)
- ◻️ Issue8: 턴 알림 시스템 (WebSocket `/turn`)
- ◻️ Issue9: 실시간 채팅 (WebSocket `/chat`)

### Phase 4: 녹음 및 분석
- 🔄 Issue10: S3 Presigned URL 발급 API
- 🔄 Issue11: 쉐도잉 리포트 생성 API (`/analyze`)
- ◻️ Issue12: AI 분석 연동 (RabbitMQ)
- ◻️ Issue13: 리포트 조회 API

### Phase 5: 세션 종료
- ✅ 학습 종료 및 방 상태 초기화 (`/finish` API)

### Phase 6: 고급 기능
- ✅ Grace Period 기반 재접속 및 자동 퇴장 처리
- ◻️ Issue15: WebRTC 시그널링 (필요시)
- ◻️ Issue16: 에러 복구 및 타임아웃 처리

---

## 7. 아카이브: 초기 고려사항

> 아래는 `room.md` 초기 버전의 내용으로, 현재는 대부분 해결되었으나 기록을 위해 남겨둡니다.

**비즈니스 로직 명확화 질문**
- 방장 퇴장 시? → 다른 사람에게 위임
- 마지막 사람 퇴장 시? → 방 자동 삭제
- 학습 중 입장? → 불가능
- 역할 중복 선택? → 기존 역할 해제 후 새 역할 선점
- 선점 안 된 역할? → 시스템(AI)이 담당

**PostgreSQL ↔ Redis 동기화 전략**
- 입장/퇴장 시 두 DB에 모두 반영.
- 트랜잭션 실패 시 롤백 필요 (현재는 `try-catch`로 처리, 추후 보강).
- 참여자 수는 Redis `SCARD` 사용.
