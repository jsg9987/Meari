<div align="center">

![logo-dark-2.svg](readme/logo-dark-2.svg)

</div>

# MEARI Backend (meari-be)

MEARI 프로젝트의 백엔드 서비스입니다. 음성/회화 연습(Shadowing), 실시간 WebRTC 연습방, 발음·억양 분석 파이프라인, 리포트/대시보드를 담당합니다.

<br><br><br>

---

<div align="center">

<h2>목차</h2>

</div>

<div align="center">

**[주요 기능](#주요-기능)**

**[기술 스택](#기술-스택)**

**[핵심 플로우](#핵심-플로우)**

**[상태/페이즈 정의](#상태페이즈-정의)**

**[분석 파이프라인](#분석-파이프라인)**

**[Redis 세션 관리 개요](#redis-세션-관리-개요)**

**[S3/미디어 흐름](#s3미디어-흐름)**

**[커서 페이지네이션 규칙](#커서-페이지네이션-규칙)**

**[운영/보안 포인트](#운영보안-포인트)**

**[실행 요구 사항](#실행-요구-사항)**

**[로컬 실행](#로컬-실행)**

**[환경 변수](#환경-변수)**

**[기본 포트](#기본-포트)**

**[문서/참고](#문서참고)**

**[프로젝트 구조](#프로젝트-구조)**

</div>

<br><br><br>

---

<div align="center">

<h2>주요 기능</h2>

</div>

- Shadowing 콘텐츠/문장/역할 관리
- 실시간 연습방(입장/진행/채팅/녹화)과 WebRTC 연동(OpenVidu)
- 발음·억양 분석(HTTP 또는 RabbitMQ 기반 비동기 처리)
- KOPIC 리포트 및 학습 결과 리포트 제공
- 미디어 업로드 및 Presigned URL 발급(S3), 이미지 업로드(Cloudinary)
- Redis 기반 실시간 상태 관리(방 상태/준비/역할/라운드 진행)
- WebSocket(STOMP) 실시간 이벤트(준비/역할/채팅/시청 완료/녹음 완료)
- KOPIC 통합 리포트 흐름(통합 리포트 생성 → 문항 평가 → 집계)
- 커서 기반 리포트/활동 조회
- 관리자 기능(스크립트 CSV 업로드, KOPIC 이미지 업로드, 콘텐츠 오디오 전처리)

<br>

---

<div align="center">

<h2>기술 스택</h2>

</div>

- Java 21, Spring Boot 3.5.9, Gradle
- PostgreSQL, Redis, RabbitMQ
- WebSocket(STOMP), OpenVidu(WebRTC)
- AWS S3, Cloudinary
- SpringDoc(OpenAPI/Swagger), JPA/Hibernate
- KOMORAN, Gemini/OpenAI/Claude 연동

<br>

---

<div align="center">

<h2>핵심 플로우</h2>

</div>

### 1) 실시간 방/섀도잉 플로우

| 단계 | 설명 |
|---|---|
| 1 | 방 생성 |
| 2 | 입장/준비 |
| 3 | 영상 시청(WATCHING) |
| 4 | 역할 선택(ROLE_PICK) |
| 5 | 라운드 진행(ROUND_1/ROUND_2) |
| 6 | 녹음 완료 |
| 7 | 분석 요청 |
| 8 | 리포트 생성 |

### 2) 발음·억양 분석 플로우

| 단계 | 설명 |
|---|---|
| 1 | 녹음 완료 시점에 분석 요청 |
| 2 | HTTP 또는 RabbitMQ 경로로 전달 |
| 3 | 분석 결과 수신 |
| 4 | `ShadowingReport` 업데이트 및 조회 |

### 3) KOPIC 평가 플로우

| 단계 | 설명 |
|---|---|
| 1 | 통합 리포트 생성 |
| 2 | 문항별 음성 업로드/평가 |
| 3 | 개별 리포트 완료 |
| 4 | 통합 리포트 집계 |

<br>

---

<div align="center">

<h2>상태/페이즈 정의</h2>

</div>

### RoomStatus

- `WAITING`: 대기 중
- `IN_PROGRESS`: 진행 중
- `COMPLETED`: 종료

### GamePhase

- `WATCHING`: 영상 시청
- `ROLE_PICK`: 역할 선택
- `ROUND_1`: 1라운드
- `ROUND_2`: 2라운드

<br>

---

<div align="center">

<h2>분석 파이프라인</h2>

</div>

- `analysis.mode` 값에 따라 분석 요청 방식이 바뀝니다.
- `http`: Spring Boot → FastAPI 직접 호출
- `rabbitmq`: Spring Boot → RabbitMQ → FastAPI
- 분석 결과는 `ShadowingReport`에 반영됩니다.

<br>

---

<div align="center">

<h2>Redis 세션 관리 개요</h2>

</div>

- 방 참여자, 준비 상태, 역할 선택, 진행 페이즈, 콘텐츠 선택 상태를 Redis에 저장합니다.
- 라운드별 녹음 완료/시청 완료 상태를 추적하여 브로드캐스트 타이밍을 결정합니다.
- WebSocket 세션과 memberId 매핑을 저장해 비정상 종료를 처리합니다.

<br>

---

<div align="center">

<h2>S3/미디어 흐름</h2>

</div>

- 녹음 파일 업로드용 Presigned URL을 발급합니다.
- 콘텐츠 영상은 Presigned URL로 안전하게 조회합니다.
- KOPIC 음성 파일은 S3 업로드 후 분석에 사용됩니다.

<br>

---

<div align="center">

<h2>커서 페이지네이션 규칙</h2>

</div>

- `cursor`: 마지막 아이템의 기준값(예: `createdAt`의 epoch ms)
- `size`: 페이지 크기(기본값 10)
- 다음 페이지 여부는 `hasNext`로 판단합니다.

<br>

---

<div align="center">

<h2>운영/보안 포인트</h2>

</div>

- 민감 정보(키/시크릿)는 환경 변수로 주입합니다.
- WebSocket 연결 시에도 `Authorization: Bearer <ACCESS_TOKEN>` 헤더가 필요합니다.

<br>

---

<div align="center">

<h2>실행 요구 사항</h2>

</div>

- Java 21
- PostgreSQL 15
- Redis
- RabbitMQ 3.x
- OpenVidu 2.30+
- Docker/Docker Compose (선택)

<br>

---

<div align="center">

<h2>로컬 실행</h2>

</div>

1. 환경 변수/설정 확인
기본값은 `src/main/resources/application.yml`에 있습니다.
예시는 `.env.example` 참고

2. 인프라 실행(선택)

```powershell
# meari-be 디렉터리에서
# Docker Compose v2
docker compose up -d
```

3. 애플리케이션 실행

```powershell
# Windows
.\gradlew.bat bootRun

# macOS/Linux
./gradlew bootRun
```

<br>

---

<div align="center">

<h2>환경 변수</h2>

</div>

필수(환경에 맞게 설정):

- DB 연결 정보: `spring.datasource.*`
- Redis: `spring.data.redis.*`
- RabbitMQ: `spring.rabbitmq.*`
- JWT: `jwt.secret-key`
- OpenVidu(WebRTC 사용 시): `OPENVIDU_URL`, `OPENVIDU_SECRET`

선택:

- S3: `AWS_S3_BUCKET`, `AWS_REGION`, `AWS_ACCESS_KEY`, `AWS_SECRET_KEY`
- Cloudinary: `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY`, `CLOUDINARY_API_SECRET`
- AI 분석: `GEMINI_API_KEY`, `GMS_KEY`

예시:

```env
OPENVIDU_URL=http://localhost:4443
OPENVIDU_SECRET=YOUR_OPENVIDU_SECRET
AWS_S3_BUCKET=YOUR_S3_BUCKET
AWS_REGION=YOUR_REGION
AWS_ACCESS_KEY=YOUR_ACCESS_KEY
AWS_SECRET_KEY=YOUR_SECRET_KEY
GEMINI_API_KEY=YOUR_GEMINI_KEY
GMS_KEY=YOUR_GMS_KEY
CLOUDINARY_CLOUD_NAME=YOUR_CLOUD_NAME
CLOUDINARY_API_KEY=YOUR_CLOUDINARY_KEY
CLOUDINARY_API_SECRET=YOUR_CLOUDINARY_SECRET
```

<br>

---

<div align="center">

<h2>기본 포트</h2>

</div>

- API 서버: `8080`
- PostgreSQL: `5433`
- Redis: `6380`
- RabbitMQ: `5673` (docker-compose 기준, 컨테이너 내부는 5672)
- OpenVidu: `4443`

RabbitMQ를 `docker-compose.yml` 그대로 사용하면 호스트 포트가 `5673`입니다.
애플리케이션을 호스트에서 실행할 경우 `spring.rabbitmq.port`를 `5673`으로 맞추거나 compose 포트를 `5672:5672`로 변경하세요.

<br>

---

<div align="center">

<h2>문서/참고</h2>

</div>

- API 요약: `API.md`
- 프로젝트 전체 소개: `readme/README_project.md`
- 개발 가이드: `HELP.md`

<br>

---

<div align="center">

<h2>프로젝트 구조</h2>

</div>

- `com.ssafy.meari.domain`: 도메인 기능(Controller/Service/Repository/Entity/DTO)
- `com.ssafy.meari.global`: 공통 설정, 인증/인가, 예외 처리, 공통 유틸

상세 구조는 `PROJECT_STRUCTURE.md`를 참고하세요.

<br><br>

---

<div align="center">

<br>

### 라이센스

<br>

SSAFY(삼성 청년 소프트웨어 아카데미) 14기 공통 프로젝트

**C207 Meari**

</div>

<br>
<br>

---

**마지막 업데이트**: 2026-02-23
