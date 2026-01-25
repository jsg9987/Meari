# Shadowing 기능 구현 작업 문서

## 1. 개요

4인 준실시간 한국어 쉐도잉 학습 방 기능 구현

### 핵심 플로우
```
방 생성 → 입장 → 준비 → [시작] → 동영상 선택 → 시청 → 역할 선택 → Round1 → Round2 → 준비로 회귀
                       ↑
                  여기서부터 입장 불가
```

---

## 2. Entity 수정 사항 (완료)

### Room Entity
- ❌ `content_id` FK 제거 (동영상은 게임 시작 후 선택)
- ✅ `theme_id` FK 추가 (방 목록 테마 필터링용)
- ✅ `maxPeople` 추가 (2~4명)
- ✅ `status` (is_active → status로 변경)
- ✅ 도메인 메서드 추가: `updateOwner()`, `updateStatus()`, `isPasswordMatch()`, `isJoinable()`

### ShadowingReport Entity
- ❌ `audioUrl` 제거 (content_id로 조인 가능)
- ✅ `round` 추가 (Integer: 1 또는 2)
- ✅ 도메인 메서드 추가: `updateAnalysisResult()`, `markAsFailed()`, `isCompleted()`

### Member Entity
- ❌ `sex` 컬럼 제거
- ✅ 도메인 메서드 추가: `updateNickname()`, `updateProfileUrl()`, `updatePassword()`, `updateNativeLanguage()`

### ReportStatus Enum
- ✅ `FAILED` 상태 추가

---

## 3. 비즈니스 로직 결정 사항

### RoomStatus 상태 관리
| 상태 | 설명 | 입장 가능 |
|------|------|----------|
| WAITING | 대기 중 (준비 단계) | O |
| IN_PROGRESS | 진행 중 (시작~학습 종료) | X |
| COMPLETED | 종료됨 | X |

### 세부 진행 상태 (Redis 관리)
```redis
room:{room_id}:phase = "SELECTING" | "WATCHING" | "ROLE_PICK" | "ROUND_1" | "ROUND_2"
room:{room_id}:content_id = 현재 선택된 콘텐츠 ID
```

### 퇴장/연결 끊김 처리
| 상황 | 처리 |
|------|------|
| 방장 퇴장 (WAITING) | 가장 먼저 입장한 사람에게 위임 |
| 방장 퇴장 (IN_PROGRESS) | 위임 + 학습 계속 |
| 일반 유저 퇴장 (IN_PROGRESS) | 해당 역할 → AI가 담당 |
| 마지막 1명 퇴장 | 방 자동 삭제 (COMPLETED) |
| 네트워크 끊김 | 10초 Grace Period 후 퇴장 처리 |

### 역할 선점
- Redis `HSETNX`로 원자성 보장
- 이미 선점한 역할이 있으면 기존 역할 해제 후 새 역할 선점
- 선점되지 않은 역할은 시스템(AI)이 담당

---

## 4. API 설계

### HTTP API
| Method | URL | 설명 |
|--------|-----|------|
| POST | /api/v1/rooms | 방 생성 |
| GET | /api/v1/rooms | 방 목록 조회 (커서 기반) |
| GET | /api/v1/rooms/{roomId} | 방 상세 조회 |
| POST | /api/v1/rooms/{roomId}/enter | 방 입장 |
| DELETE | /api/v1/rooms/{roomId}/leave | 방 퇴장 |
| POST | /api/v1/rooms/{roomId}/ready | 준비 상태 토글 |
| POST | /api/v1/rooms/{roomId}/start | 게임 시작 (방장) |
| POST | /api/v1/rooms/{roomId}/content | 동영상 선택 (방장) |
| POST | /api/v1/rooms/{roomId}/role | 역할 선점 |

### WebSocket Topics
| Topic | 설명 |
|-------|------|
| /topic/room/{roomId}/state | 참여자 변경, 준비 상태, 역할 선점 |
| /topic/room/{roomId}/video-sync | 영상 동기화 |
| /topic/room/{roomId}/turn | 쉐도잉 턴 알림 |
| /topic/room/{roomId}/chat | 채팅 |

---

## 5. Redis 데이터 구조

```redis
# 방 참여자 목록
room:{roomId}:members = Set<memberId>

# 역할 선점 정보
room:{roomId}:roles = Hash { roleId -> memberId | "SYSTEM" }

# 준비 상태
room:{roomId}:ready = Hash { memberId -> "true" | "false" }

# 현재 콘텐츠
room:{roomId}:content_id = contentId

# 진행 단계
room:{roomId}:phase = "SELECTING" | "WATCHING" | "ROLE_PICK" | "ROUND_1" | "ROUND_2"

# 연결 끊김 추적 (Grace Period)
room:{roomId}:disconnected = Hash { memberId -> timestamp }

# TTL 설정 (24시간)
EXPIRE room:{roomId}:* 86400
```

---

## 6. 분석 처리 플로우

### 문장별 분석
```
1. 문장 발화 끝 → S3에 개별 파일 업로드
2. Redis에 문장 상태 마킹 (PROCESSING)
3. AI 서버로 분석 요청 (RabbitMQ)
4. 분석 완료 → Redis에 결과 저장 + COMPLETED 마킹
5. 모든 문장 완료 확인 → PostgreSQL에 shadowing_report INSERT
```

### 분석 상태 추적 (Redis)
```redis
report:{roomId}:{memberId}:{round}:sentences = Hash { sentenceId -> "PROCESSING" | "COMPLETED" | "FAILED" }
report:{roomId}:{memberId}:{round}:result:{sentenceId} = JSON { accuracy, intonation, detail }
```

### detailed_analysis JSON 구조
```json
{
  "sentences": [
    {
      "sentence_id": 11,
      "sequence": 1,
      "text_ko": "어서오세요",
      "audio_url": "s3://.../sentence_11.wav",
      "accuracy": 85,
      "intonation": 90,
      "accuracy_detail": {
        "answer_phonemes": ["ㅇ", "ㅓ", "ㅅ", "ㅓ"],
        "predict_phonemes": ["ㅇ", "ㅓ", "ㅅ", "ㅓ"],
        "predict_probability": [0.95, 0.92, 0.88, 0.91]
      },
      "intonation_detail": {
        "answer_intonation": [120, 135, 140],
        "member_intonation": [118, 133, 142]
      }
    }
  ],
  "summary": {
    "total_sentences": 5,
    "avg_accuracy": 82,
    "avg_intonation": 87
  }
}
```

---

## 7. 구현 진행 상황

### Issue 1: Room CRUD
- [ ] RoomRepository 생성
- [ ] RoomService 생성
- [ ] RoomController 생성
- [ ] DTO 클래스 생성
- [ ] ErrorCode 추가
- [ ] 테스트 코드 작성

### Issue 2: Redis + WebSocket 설정
- [ ] WebSocketConfig
- [ ] Redis 세션 관리 서비스
- [ ] STOMP 메시지 핸들러

### Issue 3: 입장/퇴장 및 상태 동기화
- [ ] 입장 권한 확인 로직
- [ ] 퇴장 처리 로직
- [ ] WebSocket 브로드캐스트

### Issue 4: 역할 선점 시스템
- [ ] Redis HSETNX 구현
- [ ] 역할 변경 로직
- [ ] WebSocket 알림

---

## 8. 참고 사항

### CLAUDE.md 원칙 준수
- Layered Architecture: Controller → Service → Repository → Entity
- TDD 우선: 테스트 코드 먼저 작성
- @RequiredArgsConstructor 사용
- ApiResponse 공통 응답
- BusinessException 예외 처리
- Swagger 어노테이션
- Slf4j 로깅

### 네이밍 컨벤션
- Entity: PascalCase (Room, Member)
- DTO: {Entity}{Action}Request/Response (RoomCreateRequest)
- Service: {Entity}Service
- Repository: {Entity}Repository

---

## 9. 메모

### AI 서버 관련
- FastAPI에서 모델 상주 (Warm Start) 필수
- 서버 시작 시 모델 1회 로드 → 요청마다 재사용
- 예상 처리 시간: 2~3초/문장

### 주의사항
- PostgreSQL 트랜잭션 실패 시 Redis 롤백 필요
- WebSocket disconnect 이벤트 처리 필수
- Grace Period로 일시적 끊김 vs 실제 퇴장 구분

---

*마지막 업데이트: 2026-01-25*
