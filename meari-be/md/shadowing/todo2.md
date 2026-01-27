# Shadowing 기능 작업 현황 (2026-01-27)

## 완료된 작업

### 1. WebSocket API 구현
- ✅ WebSocket 연결 설정 (`/ws` 엔드포인트)
- ✅ 준비 상태 토글 (`/app/rooms/{roomId}/ready`)
- ✅ 역할 선택 (`/app/rooms/{roomId}/roles/select`)
- ✅ 역할 해제 (`/app/rooms/{roomId}/roles/release`)
- ✅ 실시간 브로드캐스트 메시지 처리
- ✅ 에러 처리 (`/queue/errors`)

### 2. SecurityConfig 설정
- ✅ WebSocket 엔드포인트 인증 제외 처리 (`/ws/**`)
- ✅ JWT 필터 체인 구성
- ✅ CORS 설정

### 3. ErrorCode 정의
- ✅ 방 관련 에러 코드 (NOT_ROOM_MEMBER, NOT_ROOM_OWNER 등)
- ✅ 역할 관련 에러 코드 (ROLE_ALREADY_TAKEN, ROLE_NOT_SELECTED 등)
- ✅ 게임 진행 관련 에러 코드 (INVALID_PHASE, CONTENT_NOT_SELECTED 등)
- ✅ 준비 상태 에러 코드 (OWNER_CANNOT_READY, NOT_ALL_READY)

### 4. 게임 시작 플로우
- ✅ 동영상 선택 플로우 변경 (contentId 요청 추가)
- ✅ RoomSessionService에 getContentId 메서드 추가
- ✅ WebSocket 브로드캐스트 추가

## 현재 작업 상태

### 브랜치: `be/feature/room-crud`

### 최근 커밋
- c002647: [BE] docs(shadowing): 플로우 변경 및 WebSocket 구현 완료 기록
- f44233f: [BE] feat(room/controller): 게임 시작 API에 contentId 요청 추가
- bfd0d2a: [BE] feat(room/service): 동영상 선택 플로우 변경 및 WebSocket 브로드캐스트 추가

## 핵심 구현 내용

### WebSocket 아키텍처
- **프로토콜**: STOMP over SockJS
- **인증**: JWT 토큰 기반
- **저장소**: Redis (임시 상태 저장)
- **영구 저장**: Round1 시작 시 ShadowingReport에 DB 저장
- **동시성 제어**: Redis HSETNX로 역할 선점 원자성 보장

### 주요 기능
1. **준비 상태 관리**: 방장 제외, 참여자만 준비/취소 가능
2. **역할 선택**: Redis 기반 선점 메커니즘, 동시 선택 방지
3. **실시간 브로드캐스트**: 같은 방 모든 참여자에게 상태 변경 알림
4. **연결 관리**: Grace Period 기반 재연결 처리

## 참고 문서
- `md/shadowing/websocket-api.md`: WebSocket API 명세
- `src/main/java/com/ssafy/meari/global/config/SecurityConfig.java`: 보안 설정
- `src/main/java/com/ssafy/meari/global/error/ErrorCode.java`: 에러 코드 정의

## 다음 작업 (필요시)
- [ ] 테스트 코드 검증 및 실행
- [ ] WebSocket 연결 해제 시 자동 퇴장 처리 검증
- [ ] Redis 데이터 정합성 확인
- [ ] DB 저장 시점(Round1) 로직 검증
