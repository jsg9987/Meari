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

## 완료한 작업

### 2026-01-26 오후: WebSocket 브로드캐스트 + 플로우 변경 ✅

#### 주요 변경 사항
**플로우 개선:**
- 기존: 준비 → 시작 → 동영상 선택
- 변경: 동영상 선택 → 준비 → 시작
- 이유: UX 개선 (모두가 동영상 확인 후 준비, 시작 시 즉시 다운로드)

**WebSocket 브로드캐스트 추가:**
- 6개 API에 실시간 알림 추가 (입장/퇴장/준비/시작/동영상선택/역할선점)
- `/topic/room/{roomId}/state` 토픽 사용

#### 추가된 파일
- `GameStartRequest.java` - 게임 시작 요청 DTO (contentId 포함)

#### 수정된 파일
- `RoomStateMessage.java` - contentId 필드 + contentSelected, gameStart 메서드 추가
- `RoomService.java` - 6개 메서드에 브로드캐스트 추가, startGame에 contentId 검증
- `RoomController.java` - startGame에 GameStartRequest 추가, API 순서 변경
- `RoomSessionService.java` - getContentId() 메서드 추가
- `ErrorCode.java` - 3개 에러코드 추가 (CONTENT_NOT_SELECTED, CONTENT_MISMATCH, CONTENT_SELECT_ONLY_WAITING)
- `work.md` - 플로우, API 순서, phase 정보 업데이트

---

### 2026-01-26 오전: HTTP API 전체 완료 ✅

#### 커밋 내역
```
aaebd31 [BE] docs(shadowing): 작업 진행 문서 추가
b2fb1f6 [BE] feat(room): 방 상태 관리 API 추가
16a9a56 [BE] feat(room/dto): 동영상 선택 요청 DTO 추가
2c078ad [BE] feat(content/repository): Content, Role Repository 추가
```

#### HTTP API 전체 완료
| API | 설명 |
|-----|------|
| POST /api/v1/rooms | 방 생성 |
| GET /api/v1/rooms | 방 목록 조회 |
| GET /api/v1/rooms/{roomId} | 방 상세 조회 |
| POST /api/v1/rooms/{roomId}/enter | 방 입장 |
| DELETE /api/v1/rooms/{roomId}/leave | 방 퇴장 |
| POST /api/v1/rooms/{roomId}/content | 동영상 선택 (WAITING) |
| POST /api/v1/rooms/{roomId}/ready | 준비 상태 토글 |
| POST /api/v1/rooms/{roomId}/start | 게임 시작 (contentId) |
| POST /api/v1/rooms/{roomId}/role | 역할 선점 |

#### 추가된 파일
- `ContentSelectRequest.java` - 동영상 선택 요청 DTO
- `ContentRepository.java` - 콘텐츠 존재 확인용
- `RoleRepository.java` - 역할 존재 확인용

---

## 다음 작업

### Issue 5: 영상 동기화 (Video Sync)
- [ ] WebSocket `/topic/room/{roomId}/video-sync` 구현
- [ ] 재생/일시정지/탐색 이벤트 브로드캐스트
- [ ] 시간 동기화 로직

### Issue 6: 쉐도잉 턴 관리
- [ ] WebSocket `/topic/room/{roomId}/turn` 구현
- [ ] 문장별 턴 할당 및 알림
- [ ] Round 진행 상태 관리

### Issue 7: 음성 녹음 및 분석 연동
- [ ] S3 업로드 API
- [ ] RabbitMQ 메시지 발행
- [ ] 분석 결과 수신 및 Redis 저장
- [ ] PostgreSQL 최종 리포트 저장

---

## 참고

- `work.md`: 전체 설계, Redis 구조, API 설계
- `CLAUDE.md`: 프로젝트 컨벤션, 아키텍처 원칙
