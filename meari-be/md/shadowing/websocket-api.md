# WebSocket API 문서

## 연결 정보

- **프로토콜**: WebSocket (STOMP over SockJS)
- **Endpoint**: `ws://{host}/ws`
- **인증**: JWT 토큰 필요 (연결 시 쿼리 파라미터 또는 헤더로 전달)

---

## 구독 (Subscribe) 엔드포인트

클라이언트는 다음 토픽을 구독하여 실시간 업데이트를 받을 수 있습니다.

### `/topic/rooms/{roomId}`

방의 모든 실시간 이벤트를 수신합니다.

**수신 메시지 타입:**
- 준비 상태 변경
- 역할 선택/해제
- 참여자 입장/퇴장
- 게임 시작
- 기타 방 상태 변경

---

## 메시지 전송 (Send) 엔드포인트

### 1. 준비 상태 토글

**Destination**: `/app/rooms/{roomId}/ready`

**요청 본문**: 없음 (멤버 정보는 JWT에서 추출)

**응답 브로드캐스트**:
```json
{
  "type": "READY_TOGGLE",
  "roomId": 1,
  "memberId": 123,
  "ready": true,
  "timestamp": "2026-01-27T10:30:00"
}
```

**설명**: 방 참여자가 준비 완료/취소를 토글합니다. 방장은 준비 상태를 변경할 수 없습니다.

**제약 조건**:
- 방 참여자만 호출 가능
- 방장은 호출 불가 (ErrorCode: `OWNER_CANNOT_READY`)

---

### 2. 역할 선택

**Destination**: `/app/rooms/{roomId}/roles/select`

**요청 본문**:
```json
{
  "role_id": 1
}
```

**필드 설명**:
- `role_id` (Long, 필수): 선택할 역할 ID (1~4)

**응답 브로드캐스트** (성공):
```json
{
  "type": "ROLE_SELECTED",
  "roomId": 1,
  "memberId": 123,
  "roleId": 1,
  "success": true,
  "timestamp": "2026-01-27T10:31:00"
}
```

**응답 브로드캐스트** (실패):
```json
{
  "type": "ROLE_SELECTED",
  "roomId": 1,
  "memberId": 123,
  "roleId": 1,
  "success": false,
  "message": "이미 선점된 역할입니다.",
  "timestamp": "2026-01-27T10:31:00"
}
```

**설명**: 역할을 선점합니다. Redis에서 원자적으로 처리되며, 이미 선점된 역할은 선택할 수 없습니다.

**제약 조건**:
- 방 참여자만 호출 가능
- 게임 진행 중 (ROLE_PICK 단계)에만 가능
- 한 번에 하나의 역할만 선택 가능 (기존 역할은 자동 해제)

---

### 3. 역할 해제

**Destination**: `/app/rooms/{roomId}/roles/release`

**요청 본문**: 없음 (멤버 정보는 JWT에서 추출)

**응답 브로드캐스트**:
```json
{
  "type": "ROLE_RELEASED",
  "roomId": 1,
  "memberId": 123,
  "roleId": 1,
  "timestamp": "2026-01-27T10:32:00"
}
```

**설명**: 현재 선택한 역할을 해제합니다.

**제약 조건**:
- 방 참여자만 호출 가능
- 역할을 선택한 상태에서만 호출 가능

---

## 일반 에러 응답

WebSocket 메시지 처리 중 에러 발생 시, 해당 사용자에게만 에러 메시지를 전송합니다.

**Destination**: `/queue/errors` (개인)

**에러 응답 형식**:
```json
{
  "type": "ERROR",
  "code": "NOT_ROOM_MEMBER",
  "message": "방 참여자가 아닙니다.",
  "timestamp": "2026-01-27T10:33:00"
}
```

---

## 연결 라이프사이클

### 연결 수립
1. 클라이언트가 `/ws` 엔드포인트에 WebSocket 연결
2. JWT 토큰 인증 수행
3. 연결 성공 시 `/topic/rooms/{roomId}` 구독

### 연결 해제
1. 정상 종료: 클라이언트가 연결 종료
2. 비정상 종료: Grace Period 동안 재연결 대기 (Redis에 마킹)
3. 재연결 시간 초과 시 자동 퇴장 처리

---

## 주의사항

1. **인증**: 모든 WebSocket 메시지는 JWT 인증이 필요합니다.
2. **브로드캐스트**: 대부분의 메시지는 같은 방의 모든 참여자에게 브로드캐스트됩니다.
3. **Redis 저장**: WebSocket으로 전송된 데이터는 Redis에만 저장되며, DB에는 저장되지 않습니다.
4. **DB 저장 시점**: Round1 시작 시 역할 정보가 DB(ShadowingReport)에 영구 저장됩니다.
5. **원자성**: 역할 선점은 Redis의 HSETNX로 원자적으로 처리되어 동시성 문제를 방지합니다.
