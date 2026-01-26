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

## 완료한 작업 (2026-01-26)

### 커밋 내역
```
aaebd31 [BE] docs(shadowing): 작업 진행 문서 추가
b2fb1f6 [BE] feat(room): 방 상태 관리 API 추가
16a9a56 [BE] feat(room/dto): 동영상 선택 요청 DTO 추가
2c078ad [BE] feat(content/repository): Content, Role Repository 추가
```

### HTTP API 전체 완료
| API | 설명 |
|-----|------|
| POST /api/v1/rooms | 방 생성 |
| GET /api/v1/rooms | 방 목록 조회 |
| GET /api/v1/rooms/{roomId} | 방 상세 조회 |
| POST /api/v1/rooms/{roomId}/enter | 방 입장 |
| DELETE /api/v1/rooms/{roomId}/leave | 방 퇴장 |
| POST /api/v1/rooms/{roomId}/ready | 준비 상태 토글 |
| POST /api/v1/rooms/{roomId}/start | 게임 시작 |
| POST /api/v1/rooms/{roomId}/content | 동영상 선택 |
| POST /api/v1/rooms/{roomId}/role | 역할 선점 |

### 추가된 파일
- `ContentSelectRequest.java` - 동영상 선택 요청 DTO
- `ContentRepository.java` - 콘텐츠 존재 확인용
- `RoleRepository.java` - 역할 존재 확인용

### 수정된 파일
- `RoomService.java` - toggleReady, startGame, selectContent, selectRole 추가
- `RoomController.java` - 4개 엔드포인트 추가
- `ErrorCode.java` - 5개 에러코드 추가

---

## 다음 작업

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

- `work.md`: 전체 설계, Redis 구조, API 설계
- `CLAUDE.md`: 프로젝트 컨벤션, 아키텍처 원칙
