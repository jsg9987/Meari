# Shadowing 기능 - 다음 작업

## 다른 컴퓨터에서 Claude로 이어서 작업하기

### 1. 새 세션 시작 시 Claude에게 보낼 프롬프트
```
meari-be/md/shadowing/work.md 읽고 현재 진행 상황 파악해줘.
그리고 CLAUDE.md도 읽어서 프로젝트 컨벤션 확인해.
```

### 2. 작업 이어서 진행할 때
```
work.md에서 남은 작업 이어서 진행해줘.
API 하나씩 Controller - Service 순서로 개발해줘.
```

---

## 오늘 완료한 작업 (2026-01-26)

### HTTP API 전체 완료
- [x] POST /api/v1/rooms/{roomId}/ready - 준비 상태 토글
- [x] POST /api/v1/rooms/{roomId}/start - 게임 시작
- [x] POST /api/v1/rooms/{roomId}/content - 동영상 선택
- [x] POST /api/v1/rooms/{roomId}/role - 역할 선점

### 추가된 파일
- `ContentSelectRequest.java`
- `ContentRepository.java`
- `RoleRepository.java`

### 수정된 파일
- `RoomService.java` - toggleReady, startGame, selectContent, selectRole 메서드 추가
- `RoomController.java` - 4개 엔드포인트 추가
- `ErrorCode.java` - OWNER_CANNOT_READY, ROOM_NOT_WAITING, ROOM_NOT_IN_PROGRESS, NOT_ALL_READY, INVALID_PHASE 추가

---

## 다음 작업 (Issue 2~4 남은 부분)

### Issue 2: WebSocket 설정
- [ ] WebSocketConfig 생성
- [ ] STOMP 메시지 핸들러 구현

### Issue 3: WebSocket 브로드캐스트
- [ ] 입장/퇴장 시 상태 브로드캐스트
- [ ] 준비 상태 변경 시 브로드캐스트
- [ ] 게임 시작 시 브로드캐스트

### Issue 4: 역할 선점 WebSocket
- [ ] 역할 선점/해제 시 브로드캐스트

---

## 참고

work.md에 전체 설계와 Redis 구조가 있으니 꼭 먼저 읽을 것.
