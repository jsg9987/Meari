# MEARI Backend (meari-be)

MEARI 프로젝트의 백엔드 서비스입니다. 음성/회화 연습(Shadowing), 실시간 WebRTC 연습방, 발음·억양 분석 파이프라인, 리포트/대시보드를 담당합니다.

## 목차
- 주요 기능
- 기술 스택
- 실행 요구 사항
- 로컬 실행
- 환경 변수
- 기본 포트
- 문서/참고
- 프로젝트 구조

## 주요 기능
- Shadowing 콘텐츠/문장/역할 관리
- 실시간 연습방(입장/진행/채팅/녹화)과 WebRTC 연동(OpenVidu)
- 발음·억양 분석(HTTP 또는 RabbitMQ 기반 비동기 처리)
- KOPIC 리포트 및 학습 결과 리포트 제공
- 미디어 업로드 및 Presigned URL 발급(S3), 이미지 업로드(Cloudinary)

## 기술 스택
- Java 21, Spring Boot 3.5.9, Gradle
- PostgreSQL, Redis, RabbitMQ
- WebSocket(STOMP), OpenVidu(WebRTC)
- AWS S3, Cloudinary
- SpringDoc(OpenAPI/Swagger), JPA/Hibernate
- KOMORAN, Gemini/OpenAI/Claude 연동

## 실행 요구 사항
- Java 21
- PostgreSQL 15
- Redis
- RabbitMQ 3.x
- OpenVidu 2.29+
- Docker/Docker Compose (선택)

## 로컬 실행
1. 환경 변수/설정 확인
- 기본값은 `src/main/resources/application.yml`에 있습니다.
- 예시는 `.env.example` 참고

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

## 환경 변수
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

## 기본 포트
- API 서버: `8080`
- PostgreSQL: `5433`
- Redis: `6380`
- RabbitMQ: `5673` (docker-compose 기준, 컨테이너 내부는 5672)
- OpenVidu: `4443`

RabbitMQ를 `docker-compose.yml` 그대로 사용하면 호스트 포트가 `5673`입니다. 애플리케이션을 호스트에서 실행할 경우 `spring.rabbitmq.port`를 `5673`으로 맞추거나 compose 포트를 `5672:5672`로 변경하세요.

## 문서/참고
- API 문서: Swagger UI(`/swagger-ui.html`), OpenAPI JSON(`/api-docs`)
- 프로젝트 전체 소개: `readme/README_project.md`
- 개발 가이드: `HELP.md`

## 프로젝트 구조
- `com.ssafy.meari.domain`: 도메인 기능(Controller/Service/Repository/Entity/DTO)
- `com.ssafy.meari.global`: 공통 설정, 인증/인가, 예외 처리, 공통 유틸

상세 구조는 `PROJECT_STRUCTURE.md`를 참고하세요.
