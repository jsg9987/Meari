# 작업 완료 내역 (2026-01-28)

## 1. WebSocket JWT 인증 구현 ✅

### 추가된 파일
- `JwtChannelInterceptor.java` - STOMP CONNECT 시 JWT 검증 인터셉터
- `WebSocketEventListener.java` - WebSocket 연결/해제 이벤트 리스너

### 수정된 파일
- `WebSocketConfig.java` - JWT 인터셉터 등록
- `JwtAuthenticationFilter.java` - `/ws` 경로 스킵 추가
- `websocket-test.html` - Access Token 입력 필드 및 연결 시 헤더 포함

### 구현 내용
1. STOMP CONNECT 프레임에서 Authorization 헤더 검증
2. JWT 토큰 검증 및 블랙리스트 체크
3. 인증 정보를 WebSocket 세션에 저장
4. HTML 테스트 페이지에 토큰 입력 기능 추가

---

## 2. 영상 시청 완료 API 추가 ✅

### 플로우 보완
기존에 누락되어 있던 **WATCHING → ROLE_PICK** phase 전환을 구현했습니다.

### 추가된 API
**POST** `/api/v1/rooms/{roomId}/watching/finish`
- 설명: 영상 시청이 완료되어 역할 선택 단계로 전환
- 권한: 방장만 가능
- 조건: WATCHING 단계에서만 가능
- 응답: phase를 ROLE_PICK으로 변경, 브로드캐스트

### 코드 변경
- `RoomController.java` - finishWatching 엔드포인트 추가
- `RoomService.java` - finishWatching 메서드 추가
- `RoomStateMessage.java` - phaseChange 메서드 활용

### 플로우
```
게임 시작 (WATCHING)
  ↓
영상 시청
  ↓
finishWatching 호출 (방장)
  ↓
ROLE_PICK phase 전환
  ↓
역할 선택 시작
```

---

## 3. 게임 종료 및 준비 단계 복귀 API 추가 ✅

### 추가된 API
**POST** `/api/v1/rooms/{roomId}/finish`
- 설명: Round2 종료 후 준비 단계로 복귀
- 권한: 방장만 가능
- 조건: ROUND_2 단계에서만 가능
- 동작:
  1. 방 상태를 WAITING으로 변경
  2. Redis 게임 상태 초기화 (준비, 역할, phase 등)
  3. 참여자 목록은 유지
  4. 브로드캐스트

### 코드 변경
- `RoomController.java` - finishGame 엔드포인트 추가
- `RoomService.java` - finishGame 메서드 추가
- `RoomSessionService.java` - resetGameState 메서드 추가

### 플로우
```
Round2 진행 중 (ROUND_2)
  ↓
Round2 완료
  ↓
finishGame 호출 (방장)
  ↓
WAITING 상태로 복귀
  ↓
Redis 게임 데이터 초기화 (members 제외)
  ↓
다시 동영상 선택부터 시작 가능
```

---

## 4. 전체 게임 플로우 (완성)

```
1. 방 생성 (WAITING)
   ↓
2. 동영상 선택
   ↓
3. 참여자 준비
   ↓
4. 게임 시작 (IN_PROGRESS + WATCHING)
   ↓
5. 영상 시청
   ↓
6. 영상 시청 완료 (ROLE_PICK) ← 신규 추가
   ↓
7. 역할 선택
   ↓
8. 역할 확정
   ↓
9. Round1 시작 (ROUND_1)
   ↓
10. Round1 진행
   ↓
11. Round2 시작 (ROUND_2)
   ↓
12. Round2 진행
   ↓
13. 게임 종료 (WAITING으로 복귀) ← 신규 추가
   ↓
다시 2번부터 반복 가능
```

---

## 5. API 목록 (전체)

| Method | URL | 설명 | 권한 |
|--------|-----|------|------|
| POST | /api/v1/rooms | 방 생성 | 인증된 사용자 |
| GET | /api/v1/rooms | 방 목록 조회 | 인증된 사용자 |
| GET | /api/v1/rooms/{roomId} | 방 상세 조회 | 인증된 사용자 |
| POST | /api/v1/rooms/{roomId}/enter | 방 입장 | 인증된 사용자 |
| DELETE | /api/v1/rooms/{roomId}/leave | 방 퇴장 | 방 참여자 |
| POST | /api/v1/rooms/{roomId}/content | 동영상 선택 | 방장 (WAITING) |
| POST | /api/v1/rooms/{roomId}/start | 게임 시작 | 방장 (WAITING) |
| **POST** | **/api/v1/rooms/{roomId}/watching/finish** | **영상 시청 완료** | **방장 (WATCHING)** |
| POST | /api/v1/rooms/{roomId}/roles/confirm | 역할 확정 | 방장 (ROLE_PICK) |
| POST | /api/v1/rooms/{roomId}/rounds/start | Round 시작 | 방장 |
| **POST** | **/api/v1/rooms/{roomId}/finish** | **게임 종료** | **방장 (ROUND_2)** |

---

## 6. GamePhase 상태 전환 다이어그램

```
WAITING (방 대기)
  ↓ startGame
WATCHING (영상 시청)
  ↓ finishWatching ← 신규 추가
ROLE_PICK (역할 선택)
  ↓ startRound(1)
ROUND_1 (라운드 1)
  ↓ startRound(2)
ROUND_2 (라운드 2)
  ↓ finishGame ← 신규 추가
WAITING (다시 대기)
```

---

## 7. Redis 데이터 구조 (최종)

```redis
# 방 참여자 목록
room:{roomId}:members = Set<memberId>

# 역할 선점 정보
room:{roomId}:roles = Hash { roleId -> memberId | "SYSTEM" }

# 준비 상태
room:{roomId}:ready = Hash { memberId -> "true" | "false" }

# 역할 확정 플래그
room:{roomId}:roles_confirmed = "true" | "false"

# 현재 콘텐츠
room:{roomId}:content_id = contentId

# 진행 단계
room:{roomId}:phase = "WATCHING" | "ROLE_PICK" | "ROUND_1" | "ROUND_2"

# 연결 끊김 추적 (Grace Period)
room:{roomId}:disconnected = Hash { memberId -> timestamp }
```

---

## 8. 다음 작업 (필요시)

### 미완성 기능
- [ ] Grace Period 기반 자동 퇴장 처리 (WebSocketEventListener에 TODO)
- [ ] 영상 동기화 WebSocket (/topic/room/{roomId}/video-sync)
- [ ] 쉐도잉 턴 관리 (/topic/room/{roomId}/turn)
- [ ] 음성 녹음 S3 업로드
- [ ] RabbitMQ 분석 요청
- [ ] 분석 결과 Redis 저장
- [ ] PostgreSQL 최종 리포트 저장

### 테스트 필요
- [ ] finishWatching API 테스트
- [ ] finishGame API 테스트
- [ ] WebSocket JWT 인증 테스트
- [ ] Phase 전환 통합 테스트

---

## 9. 참고 문서
- `work.md` - 전체 설계 문서
- `CLAUDE.md` - 프로젝트 컨벤션
- `websocket-api.md` - WebSocket API 명세
- `todo.md` - 작업 진행 현황

---

*작업 완료 시각: 2026-01-28*
