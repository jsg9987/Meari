# Meari Backend 개발 계획서

## 프로젝트 개요

**Meari**는 4인 준실시간 한국어 쉐도잉 및 AI 발음 평가 서비스입니다.

### 핵심 기능
- 다국어 사용자를 위한 한국어 학습 플랫폼
- 실시간 화상/음성 환경에서 쉐도잉 학습
- AI 기반 발음 정밀 분석 및 피드백
- 4인까지 참여 가능한 협업 학습 방

### 기술 스택
- **Backend**: Java 21, Spring Boot 3.5.9, JPA, Spring Security (JWT + Redis)
- **Real-time**: WebSocket (STOMP), WebRTC
- **Database**: PostgreSQL (JSONB 활용), Redis
- **Storage**: AWS S3 (Presigned URL)
- **AI**: FastAPI (음성 분석 서버)

---

## 현재 코드베이스 상태

### ✅ 완료된 부분
1. **프로젝트 구조**: Layered Architecture (Controller - Service - Repository - Entity)
2. **Global 공통 기능**:
   - `ApiResponse<T>`: 표준화된 응답 형식
   - `ErrorCode` enum: 80개 이상의 상세한 에러 코드
   - `GlobalExceptionHandler`: 중앙 집중식 예외 처리
3. **설정 파일**: CORS, Redis, JWT, PostgreSQL 설정 완료
4. **문서화**: API 명세서, ERD, 프로젝트 초기화 문서 작성 완료

### ⏳ 미구현 부분
- JPA Entity 클래스 (Admin만 skeleton 존재)
- Repository, Service, Controller 로직
- JWT 인증 필터
- WebSocket 핸들러
- DTO 클래스

---

## 코딩 스타일 가이드

### 1. 패키지 구조
```
domain/{domain_name}/
├── controller/  # REST API 엔드포인트
├── service/     # 비즈니스 로직
├── repository/  # 데이터 접근 계층
├── entity/      # JPA 엔티티
└── dto/         # 요청/응답 객체
```

### 2. 네이밍 컨벤션
- **클래스**: PascalCase (예: MemberService)
- **메서드**: camelCase (예: findMemberById)
- **상수**: UPPER_SNAKE_CASE
- **패키지**: lowercase

### 3. 애노테이션 사용
- **Lombok**: `@Getter`, `@AllArgsConstructor`, `@NoArgsConstructor`, `@Builder`
- **JPA**: `@Entity`, `@Table`, `@Id`, `@GeneratedValue`, `@Column`, `@ManyToOne`, `@OneToMany`
- **Spring**: `@RestController`, `@Service`, `@Repository`, `@Configuration`

### 4. 응답 형식
```json
{
  "success": true,
  "data": { /* 실제 데이터 */ },
  "error": null
}
```

---

## Entity 개발 계획

### Phase 1: 핵심 Entity 및 공통 클래스

#### 1.1 BaseEntity 생성
- **목적**: `created_at`, `updated_at` 공통 필드 관리
- **위치**: `global/entity/BaseEntity.java`
- **기능**: `@MappedSuperclass`, `@EntityListeners(AuditingEntityListener.class)` 사용

#### 1.2 Enum 타입 정의
생성할 Enum들:
- `RoomStatus`: WAITING, IN_PROGRESS, COMPLETED (방 상태)
- `ReportStatus`: PROCESSING, COMPLETED (리포트 분석 상태)
- `NativeLanguage`: KR, VN, EN, JP 등 (모국어)
- `Sex`: M, F (성별)

#### 1.3 JSONB 타입 처리
- PostgreSQL JSONB를 JPA에서 다루기 위한 설정
- `@JdbcTypeCode(SqlTypes.JSON)` 사용 (Hibernate 6+)

### Phase 2: 도메인별 Entity 구현 순서

#### 우선순위 1: 기본 도메인 (독립적 테이블)
1. **Member** - 회원 정보
2. **Theme** - 학습 테마
3. **Word** - 단어

#### 우선순위 2: 컨텐츠 도메인
4. **Content** - 영상 콘텐츠 (Theme 의존)
5. **Role** - 역할/캐릭터 (Content 의존)
6. **Sentence** - 대사/문장 (Content, Role 의존)
7. **KopicSentence** - AI 대화 문장 (Theme 의존)

#### 우선순위 3: 관계 테이블
8. **SentenceWord** - 문장-단어 매핑 (Many-to-Many)

#### 우선순위 4: 학습 세션 도메인
9. **Room** - 쉐도잉 방 (Member, Content 의존)
10. **MemberRoom** - 방 참여 기록 (Member, Room 의존)

#### 우선순위 5: 분석 결과 도메인
11. **ShadowingReport** - 쉐도잉 분석 결과 (JSONB 포함)
12. **KopicReport** - AI 대화 분석 결과 (JSONB 포함)

### Phase 3: 관계 매핑 전략

#### 양방향 vs 단방향
- 기본적으로 **단방향 매핑**으로 시작
- 필요한 경우에만 양방향 (예: Content ↔ Sentence)
- 양방향 시 `mappedBy` 사용하여 주인 명확히 지정

#### 페치 전략
- `@ManyToOne`, `@OneToOne`: 기본 EAGER → **LAZY로 변경** 권장 (N+1 문제 방지)
- `@OneToMany`, `@ManyToMany`: LAZY 사용

#### Cascade 전략
- 신중하게 사용, 주로 부모-자식 관계에만 적용
- 예: Content 삭제 시 → Sentence, Role도 삭제 (CASCADE.ALL)

#### Orphan Removal
- 부모 엔티티에서 제거된 자식 자동 삭제
- 예: `@OneToMany(orphanRemoval = true)`

### Phase 4: 특수 요구사항 처리

#### JSONB 필드 구조 설계

**1. ShadowingReport.detailed_analysis**:
```json
{
  "accuracy_detail": {
    "answer_phonemes": ["ㅇ", "ㅏ", "ㄴ", "ㄴ", "ㅕ", "ㅇ"],
    "predict_phonemes": ["ㅇ", "ㅏ", "ㄴ", "ㄴ", "ㅕ", "ㅇ"],
    "predict_probability": [0.95, 0.92, 0.88, ...]
  },
  "intonation_detail": {
    "answer_intonation": [120, 135, 140, ...],
    "member_intonation": [118, 133, 142, ...],
    "alignment_path": [[0,0], [1,1], [2,1], ...]
  }
}
```

**2. KopicReport.detailed_analysis**:
```json
{
  "missed_point": "과거형과 희망사항이 혼용됨",
  "correction": "비빔밥 먹고 싶어요.",
  "tip": "~고 싶다 앞에는 동사 기본형 사용"
}
```

#### 타임스탬프 정밀도
- `start_time`, `end_time`, `total_duration`: `BigDecimal` 타입 사용
- `@Column(precision = 10, scale = 3)` - 밀리초 단위 지원

---

## 전체 개발 로드맵

### Step 1: Entity Layer 구축 ✅ (현재 진행 중)
- 12개 Entity 클래스 구현
- BaseEntity, Enum 클래스 생성
- 관계 매핑 및 검증

### Step 2: Repository Layer
- JpaRepository 인터페이스 정의
- Custom Query 메서드 작성
- QueryDSL 도입 검토 (복잡한 검색 조건 대비)

### Step 3: DTO Layer
- Request DTO (Validation 포함)
- Response DTO
- DTO ↔ Entity 변환 로직 (MapStruct 또는 수동 변환)

### Step 4: Service Layer
- 비즈니스 로직 구현
- Transaction 관리
- Redis 캐싱 전략

### Step 5: Controller Layer
- REST API 엔드포인트 구현
- API 명세서와 일치 확인
- Swagger/OpenAPI 문서화

### Step 6: Security Layer
- JWT 인증 필터 구현
- Spring Security 설정
- Access Token, Refresh Token 관리

### Step 7: WebSocket Layer
- STOMP 메시지 브로커 설정
- 실시간 방 상태 동기화
- WebRTC 시그널링

### Step 8: Integration
- S3 Presigned URL 생성
- RabbitMQ (Spring ↔ FastAPI)
- AI 서버 연동

---

## Entity 개발 시 고려사항

### 1. 성능 최적화
- **인덱스 전략**: `@Table(indexes = {...})` 활용
  - 자주 검색되는 컬럼: email, theme_id, content_id, room_id
- **N+1 문제 해결**: EntityGraph, Fetch Join 활용

### 2. 데이터 무결성
- **Unique 제약**: `@Column(unique = true)` - Member.email
- **Not Null 제약**: 필수 필드에 `nullable = false`
- **Foreign Key**: JPA 관계로 자동 생성

### 3. Soft Delete vs Hard Delete
- 현재 ERD에는 deleted_at 필드 없음
- 필요시 BaseEntity에 추가 고려
- 리포트 데이터는 보존 권장 (GDPR 고려)

### 4. Auditing
- `@CreatedDate`, `@LastModifiedDate` (BaseEntity)
- `@CreatedBy`, `@LastModifiedBy` (향후 추가 검토)

---

## 예상 Entity 파일 구조

```
domain/
├── member/
│   └── entity/
│       ├── Member.java
│       ├── NativeLanguage.java (enum)
│       └── Sex.java (enum)
├── theme/
│   └── entity/
│       └── Theme.java
├── content/
│   └── entity/
│       ├── Content.java
│       ├── Role.java
│       └── Sentence.java
├── word/
│   └── entity/
│       ├── Word.java
│       └── SentenceWord.java
├── room/
│   └── entity/
│       ├── Room.java
│       ├── MemberRoom.java
│       └── RoomStatus.java (enum)
├── report/
│   └── entity/
│       ├── ShadowingReport.java
│       ├── KopicReport.java
│       └── ReportStatus.java (enum)
└── kopic/
    └── entity/
        └── KopicSentence.java

global/
└── entity/
    └── BaseEntity.java
```

---

## Entity 구현 체크리스트

### BaseEntity & Enums
- [ ] BaseEntity.java
- [ ] RoomStatus.java
- [ ] ReportStatus.java
- [ ] NativeLanguage.java
- [ ] Sex.java

### Core Entities
- [ ] Member.java
- [ ] Theme.java
- [ ] Word.java

### Content Entities
- [ ] Content.java
- [ ] Role.java
- [ ] Sentence.java
- [ ] KopicSentence.java

### Relationship Entities
- [ ] SentenceWord.java

### Session Entities
- [ ] Room.java
- [ ] MemberRoom.java

### Report Entities
- [ ] ShadowingReport.java
- [ ] KopicReport.java

---

## 다음 단계

Entity 구현 완료 후:
1. Repository 인터페이스 생성
2. 기본적인 CRUD 테스트 코드 작성
3. API 명세서 기반 DTO 설계
4. Service Layer 구현 시작

---

**작성일**: 2026-01-23
**작성자**: Backend Development Team
**버전**: 1.0
