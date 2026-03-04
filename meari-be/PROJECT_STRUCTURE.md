# MEARI Backend - 프로젝트 구조 및 분석

**프로젝트명**: MEARI (메아리 - AI 기반 한국어 말하기 연습 플랫폼)
**빌드 도구**: Gradle
**Java 버전**: 21
**Spring Boot 버전**: 3.5.9

---

## 📋 목차
1. [기술 스택](#기술-스택)
2. [프로젝트 아키텍처](#프로젝트-아키텍처)
3. [디렉토리 구조](#디렉토리-구조)
4. [핵심 도메인 및 기능](#핵심-도메인-및-기능)
5. [DB 테이블 구조](#db-테이블-구조)
6. [설정 파일](#설정-파일)
7. [외부 API 통합](#외부-api-통합)

---

## 기술 스택

### Backend Framework
- **Spring Boot**: 3.5.9
- **Spring Security**: JWT 기반 인증
- **Spring Data JPA**: ORM
- **Spring WebSocket**: STOMP 통신

### Database & Cache
- **PostgreSQL**: 주 데이터베이스
- **Redis**: 세션/캐시 저장소
- **Spring Session Redis**: 분산 세션 관리

### Message Queue & Event
- **RabbitMQ**: 비동기 메시지 처리 (음성 분석 결과)

### Video & Media
- **OpenVidu 2.30.0**: WebRTC 기반 실시간 영상 통신
- **FFmpeg (JAVE)**: 동영상 처리 (3.3.1)
- **AWS S3**: 비디오 저장소
- **Cloudinary**: 이미지 업로드 서비스

### AI/ML Services
- **Gemini API** (gemini-2.5-flash): 텍스트 기반 분석
- **OpenAI GPT-4o-mini**: 음성 분석
- **Claude Opus 4.1**: 일반 분석
- **KOMORAN 3.3.4**: 한국어 자연어 처리 (NLP)

### Documentation & Validation
- **SpringDoc OpenAPI**: Swagger UI (2.8.4)
- **Jakarta Validation**: Request/Response 유효성 검증

### Development Tools
- **Lombok**: 보일러플레이트 코드 감소
- **Spring Boot DevTools**: 핫 리로드
- **JUnit 5 + Mockito**: 테스트

---

## 프로젝트 아키텍처

### 계층 구조 (Layered Architecture)
```
Controller (REST API)
    ↓
Service (비즈니스 로직)
    ↓
Repository (데이터 접근)
    ↓
Entity (도메인 모델)
```

### 설계 원칙
- **Entity와 DTO 분리**: RequestDto, ResponseDto 사용
- **생성자 주입**: `@RequiredArgsConstructor` 사용
- **Builder 패턴**: 복잡한 생성자 피하기
- **Global Exception Handling**: `@RestControllerAdvice` 기반 중앙 집중식 예외 처리
- **Dirty Checking**: JPA 변경 감지를 통한 자동 업데이트
- **TDD 원칙**: 테스트 먼저 작성 (단위 테스트 + 통합 테스트)

---

## 디렉토리 구조

```
src/main/java/com/ssafy/meari/
├── domain/                              # 비즈니스 로직 (각 도메인별)
│   ├── admin/                           # 관리자 (데이터 업로드, COPIC 이미지)
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   ├── service/
│   │   └── util/                        # CSV 파싱 유틸
│   │
│   ├── analysis/                        # 음성 분석 (RabbitMQ 기반 비동기)
│   │   ├── controller/
│   │   ├── config/                      # AnalysisServiceFactory
│   │   ├── dto/                         # AnalysisRequestMessage, AnalysisResultMessage
│   │   └── service/
│   │       ├── AnalysisConsumer         # RabbitMQ 구독
│   │       ├── AnalysisProducer         # RabbitMQ 발행
│   │       ├── HttpAnalysisService      # HTTP 기반
│   │       ├── RabbitMQAnalysisService  # RabbitMQ 기반
│   │       └── AnalysisResultService    # 결과 처리
│   │
│   ├── content/                         # 콘텐츠 (섀도잉 교재)
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/                      # Content, Role, Sentence
│   │   ├── repository/
│   │   └── service/                     # ContentService, ContentPreprocessService
│   │
│   ├── dashboard/                       # 대시보드 (사용자 활동 통계)
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/                      # DailyRecord
│   │   ├── mapper/
│   │   ├── repository/
│   │   └── service/                     # DashboardService, DashboardServiceImpl
│   │
│   ├── kopic/                           # 한국어능력시험 관련
│   │   ├── controller/                  # KopicController, KopicEvaluateController
│   │   ├── dto/
│   │   ├── entity/                      # KopicReport, KopicSentence
│   │   ├── repository/
│   │   └── service/
│   │
│   ├── member/                          # 회원 (인증/프로필)
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/                      # Member
│   │   ├── mapper/
│   │   ├── repository/
│   │   └── service/
│   │
│   ├── mypage/                          # 마이페이지 (개인 정보 관리)
│   │   ├── controller/
│   │   ├── dto/
│   │   └── service/
│   │
│   ├── report/                          # 리포트 (일반)
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   ├── repository/
│   │   └── service/
│   │
│   ├── room/                            # 실시간 협력 학습 (WebRTC)
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/                      # Room, MemberRoom
│   │   ├── interceptor/                 # WebSocket 인증
│   │   ├── repository/
│   │   └── service/
│   │
│   ├── solo_practice/                   # 개인 연습
│   │   ├── controller/
│   │   ├── dto/
│   │   ├── entity/
│   │   └── service/
│   │
│   ├── s3/                              # AWS S3 파일 저장소
│   │   ├── controller/
│   │   ├── dto/
│   │   └── service/
│   │
│   ├── cloudinary/                      # 클라우드 파일 업로드
│   │   ├── controller/
│   │   ├── dto/
│   │   └── service/
│   │
│   ├── webrtc/                          # WebRTC 관리 (OpenVidu)
│   │   ├── config/
│   │   ├── controller/
│   │   ├── dto/
│   │   └── service/
│   │
│   ├── word/                            # 단어장
│   │   ├── dto/
│   │   ├── entity/                      # Word
│   │   ├── repository/
│   │   └── service/
│   │
│   └── theme/                           # 테마 (학습 주제)
│       ├── entity/                      # Theme
│       └── repository/
│
└── global/                              # 공통 설정 및 기반 시설
    ├── auth/                            # JWT 인증
    │   ├── controller/                  # AuthController
    │   ├── jwt/                         # JWT 생성/검증
    │   ├── request/
    │   ├── response/
    │   └── service/                     # AuthService
    │
    ├── common/                          # 공통 응답 포맷
    │   ├── ApiResponse.java             # 통일된 API 응답
    │   ├── ArgumentNotValidExceptionDto
    │   ├── ExceptionDto
    │   └── CursorPageResponse
    │
    ├── config/                          # Spring 설정
    │   ├── SecurityConfig               # Spring Security 설정
    │   ├── WebSocketConfig              # WebSocket 설정
    │   └── ... (기타 설정)
    │
    ├── error/                           # 예외 처리
    │   ├── ErrorCode.java               # 에러 코드 정의
    │   ├── exception/
    │   │   └── BusinessException        # 비즈니스 로직 예외
    │   └── GlobalExceptionHandler       # 중앙 집중식 예외 처리
    │
    ├── entity/                          # 공통 엔티티
    │   ├── BaseEntity                   # 생성일, 수정일 자동 관리
    │   └── ... (기타 공통 엔티티)
    │
    ├── pipeline/                        # 비디오 처리 파이프라인
    │   └── videosaving/
    │       ├── source/                  # 비디오 소스
    │       ├── nlp/                     # 자연어 처리 (KOMORAN)
    │       ├── openai/                  # OpenAI 연동
    │       ├── anthropic/               # Claude API 연동
    │       ├── homonym/                 # 동음이의어 처리
    │       └── service/                 # 파이프라인 조직화
    │
    └── util/                           # 공통 유틸리티
```

---

## 핵심 도메인 및 기능

### 1. 회원 (Member)
- 이메일 기반 회원가입/로그인
- JWT 토큰 기반 인증
- 프로필 관리 (닉네임, 사진)
- 모국어 설정 (기본값: 한국어)

### 2. 콘텐츠 (Content)
- 섀도잉(Shadowing) 교재 관리
- 문장(Sentence) 데이터: 한국어/베트남어 번역, 타이밍 정보
- 역할(Role): 배우 캐릭터별 대사

### 3. 방 (Room) - 실시간 협력 학습
- WebRTC 기반 비디오 통신
- 최대 2-4명 협력 학습
- 방 상태: WAITING, IN_PROGRESS, COMPLETED
- 비밀번호 보호 옵션

### 4. 섀도잉 리포트 (Shadowing Report)
- 개별 라운드별 성과 평가
- 정확도(accuracy), 억양(intonation) 점수
- 상세 분석 데이터 (JSONB)
- 처리 상태: PROCESSING, COMPLETED

### 5. KOPIC 리포트 (KOPIC Report)
- 한국어능력시험 결과 관리
- 전체 점수 + 상세 분석
- 상태 관리

### 6. WebRTC & 실시간 통신
- OpenVidu 기반 영상 회의
- STOMP를 통한 메시지 전달
- 비디오 녹화 및 저장 (FFmpeg)

### 7. 음성 분석 (Analysis)
- **비동기 처리**: RabbitMQ 기반
- **팩토리 패턴**: HTTP vs RabbitMQ 선택 가능
- **AI 통합**: OpenAI, Claude, Gemini
- 발음, 억양, 이해도 분석

### 8. 단어장 (Word)
- 한국어-베트남어 단어 매핑
- 정의 포함

### 9. 대시보드 (Dashboard)
- 일일 학습 기록 (DailyRecord)
- 사용자 활동 통계
- KOPIC 점수 시각화

### 10. 파일 저장소
- **AWS S3**: 비디오 저장소
- **Cloudinary**: 이미지 업로드
- S3 Pre-signed URL: 보안 다운로드

---

## DB 테이블 구조

### 핵심 테이블

| 테이블 | 설명 | 중요 필드 |
|--------|------|---------|
| `member` | 회원 | email, password, nickname, profile_url, native_language |
| `room` | 협력 학습 방 | owner_id, theme_id, status (WAITING/IN_PROGRESS/COMPLETED), password |
| `content` | 섀도잉 교재 | theme_id, title, video_url, thumbnail_url, max_people |
| `sentence` | 문장 데이터 | content_id, role_id, sequence, start_time, end_time, text_ko, text_vn |
| `role` | 배우 역할 | content_id, 캐릭터명 |
| `theme` | 학습 주제 | name, description, theme_url |
| `shadowing_report` | 섀도잉 성과 | member_id, room_id, role_id, accuracy, intonation, detailed_analysis (JSONB), status (PROCESSING/COMPLETED) |
| `kopic_report` | 한국어능력시험 | member_id, theme_id, total_score, detailed_analysis (JSONB), status |
| `kopic_sentence` | KOPIC 문장 | theme_id, text_ko, kopic_sentence_url |
| `word` | 단어장 | word_kr, definition_kr, word_vn, definition_vn |
| `daily_record` | 일일 학습 기록 | member_id, 학습 시간, 활동 종류 |
| `member_room` | 방 참여자 | member_id, room_id |

---

## 설정 파일

### application.yml
- **서버**: Port 8080
- **DB**: PostgreSQL (localhost:5433/meari_db)
- **Redis**: localhost:6380
- **RabbitMQ**: localhost:5672 (ssafy/ssafy)
- **JWT**:
  - 액세스 토큰 만료: 43200000ms (12시간)
  - 리프레시 토큰 만료: 1209600000ms (14일)
- **파일 업로드**: 최대 50MB
- **Swagger**: `/swagger-ui.html`, `/api-docs`
- **JPA**:
  - ddl-auto: `create` (개발), 운영시 `none`
  - Dialect: PostgreSQL

### 환경 변수
```env
OPENVIDU_URL=http://localhost:4443
OPENVIDU_SECRET=MY_SECRET
AWS_S3_BUCKET=meari-bucket
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY=...
AWS_SECRET_KEY=...
GEMINI_API_KEY=...
GMS_KEY=...
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
```

---

## 외부 API 통합

### AI/ML Services
1. **Gemini API** (gemini-2.5-flash)
   - 기본 분석 및 텍스트 생성
   - Endpoint: gms.ssafy.io 게이트웨이를 통한 프록시

2. **OpenAI GPT-4o-mini**
   - 음성 분석, 발음 평가
   - Endpoint: 게이트웨이 프록시

3. **Claude Opus 4.1**
   - 고도의 분석 및 피드백
   - Endpoint: gms.ssafy.io/gmsapi/api.anthropic.com/v1/messages

4. **KOMORAN 3.3.4**
   - 한국어 형태소 분석 및 POS 태깅
   - 로컬 라이브러리 (JitPack)

### 미디어 서비스
1. **AWS S3**
   - 비디오 저장
   - Pre-signed URL로 시간 제한 다운로드
   - 동영상 만료: 1시간 / 업로드: 15분

2. **Cloudinary**
   - 이미지 업로드 및 관리
   - CDN 제공

3. **OpenVidu**
   - WebRTC 기반 실시간 영상 통신
   - 녹화 및 스트리밍

---

## 특징 및 우수 사례

### ✅ 아키텍처
- Layered Architecture로 명확한 책임 분리
- Entity와 DTO의 엄격한 분리
- Global Exception Handling으로 일관된 에러 응답

### ✅ 비동기 처리
- RabbitMQ 기반 음성 분석 (Producer/Consumer 패턴)
- AnalysisServiceFactory로 구현 선택 가능

### ✅ 보안
- JWT 토큰 기반 인증
- Spring Security 설정
- WebSocket 인터셉터를 통한 권한 검증

### ✅ 문서화
- Swagger (SpringDoc) UI
- 모든 DTO에 `@Schema` 어노테이션

### ✅ 데이터 처리
- JSONB 타입으로 복잡한 분석 결과 저장
- Redis 캐싱 및 세션 관리
- Dirty Checking을 통한 효율적인 업데이트

---

## 주요 클래스

### Global Exception Handling
- `GlobalExceptionHandler.java`: 모든 예외의 중앙 처리
- `BusinessException.java`: 비즈니스 로직 예외
- `ErrorCode.java`: 표준화된 에러 코드

### 공통 응답
- `ApiResponse<T>`: 통일된 성공/실패 응답 포맷
- ```json
  {
    "success": true/false,
    "data": {...},
    "error": {
      "code": "...",
      "message": "..."
    }
  }
  ```

### 인증
- `AuthController.java`: 회원가입, 로그인
- `JwtProvider.java`: JWT 생성 및 검증

---

## Docker 배포

프로젝트는 Docker 및 docker-compose 설정을 포함하고 있습니다:
- `Dockerfile`: Spring Boot 애플리케이션 컨테이너화
- `docker-compose.yml`: PostgreSQL, Redis, RabbitMQ 함께 실행

---

이 구조는 **TDD 원칙**, **확장성**, **유지보수성**을 고려하여 설계되었습니다.
