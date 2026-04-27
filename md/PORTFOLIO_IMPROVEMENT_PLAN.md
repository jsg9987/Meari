# 🎯 Meari 프로젝트 포트폴리오 개선 계획

> 백엔드 개발자 취업을 위한 체계적인 프로젝트 개선 로드맵

---

## 📑 목차

- [📊 현재 프로젝트 분석](#-현재-프로젝트-분석)
  - [✅ 강점](#-강점)
  - [⚠️ 개선 필요 영역](#%EF%B8%8F-개선-필요-영역)
- [🚀 개선 로드맵 (우선순위별)](#-개선-로드맵-우선순위별)
- [1️⃣ 문서화 강화](#1%EF%B8%8F⃣-문서화-강화-필수---1주)
  - [1.1 README.md 대폭 개선](#11-readmemd-대폭-개선)
  - [1.2 API 문서 자동화](#12-api-문서-자동화)
  - [1.3 아키텍처 문서 작성](#13-아키텍처-문서-작성)
- [2️⃣ 테스트 커버리지 향상](#2%EF%B8%8F⃣-테스트-커버리지-향상-필수---2주)
  - [2.1 FastAPI 단위 테스트 추가](#21-fastapi-단위-테스트-추가)
  - [2.2 Spring Boot 테스트 확장](#22-spring-boot-테스트-확장)
  - [2.3 E2E 테스트 (선택)](#23-e2e-테스트-선택)
- [3️⃣ 모니터링 & 관찰성](#3%EF%B8%8F⃣-모니터링--관찰성-중요---2주)
  - [3.1 로깅 체계화](#31-로깅-체계화)
  - [3.2 메트릭 수집 (Prometheus + Grafana)](#32-메트릭-수집-prometheus--grafana)
  - [3.3 Health Check 강화](#33-health-check-강화)
- [4️⃣ 성능 최적화](#4%EF%B8%8F⃣-성능-최적화-중요---15주)
  - [4.1 부하 테스트](#41-부하-테스트)
  - [4.2 데이터베이스 최적화](#42-데이터베이스-최적화)
  - [4.3 캐싱 전략 강화](#43-캐싱-전략-강화)
- [5️⃣ 보안 강화](#5%EF%B8%8F⃣-보안-강화-중요---1주)
  - [5.1 보안 점검 체크리스트](#51-보안-점검-체크리스트)
  - [5.2 의존성 취약점 스캔](#52-의존성-취약점-스캔)
- [6️⃣ 코드 품질 & Best Practices](#6%EF%B8%8F⃣-코드-품질--best-practices-선택---1주)
  - [6.1 정적 분석 도구 도입](#61-정적-분석-도구-도입)
  - [6.2 성능 프로파일링](#62-성능-프로파일링)
- [7️⃣ 추가 기능](#7%EF%B8%8F⃣-추가-기능-선택---면접-시-언급용)
  - [7.1 블루-그린 배포](#71-블루-그린-배포)
  - [7.2 쿠버네티스 전환 (선택)](#72-쿠버네티스-전환-선택)
  - [7.3 메시지 큐 확장성 개선](#73-메시지-큐-확장성-개선)
- [📋 우선순위 요약](#-우선순위-요약)
- [💼 이력서/포트폴리오 작성 팁](#-이력서포트폴리오-작성-팁)
- [🎓 학습 자료 추천](#-학습-자료-추천)
- [✅ 체크리스트](#-체크리스트)
- [📞 다음 단계](#-다음-단계)

---

## 📊 현재 프로젝트 분석

### ✅ 강점
- **풀스택 마이크로서비스 아키텍처**: Spring Boot + FastAPI 분리
- **실시간 통신**: WebRTC(OpenVidu) + WebSocket(STOMP)
- **비동기 메시징**: RabbitMQ 기반 이벤트 드리븐 아키텍처
- **AI/ML 통합**: Wav2Vec2 + MDD 발음 분석 모델
- **CI/CD 구축**: Jenkins + Docker 자동화 파이프라인
- **현대적 기술 스택**: Java 21, Spring Boot 3.5, PostgreSQL, Redis
- **개발 가이드라인**: CLAUDE.md에 명시된 코딩 컨벤션

### ⚠️ 개선 필요 영역
- **테스트 커버리지**: FastAPI 단위 테스트 부재
- **모니터링/관찰성**: 로깅, 메트릭 수집 시스템 없음
- **문서화**: 아키텍처 다이어그램, API 문서 보강 필요
- **성능 최적화**: 부하 테스트 및 튜닝 미비
- **보안**: 보안 감사 및 Best Practice 점검 필요

---

## 🚀 개선 로드맵 (우선순위별)

---

## 1️⃣ **문서화 강화** (필수 - 1주)

> **중요도: ★★★★★**
> 면접관이 가장 먼저 보는 부분. 기술 이해도와 커뮤니케이션 능력을 보여줌.

### 1.1 README.md 대폭 개선

**현재 상태:**
- Git 컨벤션 중심의 팀 내부 문서

**개선 목표:**
```markdown
# README.md 구조 (예시)

## 📖 프로젝트 소개
- 문제 정의 (왜 이 프로젝트를 만들었나?)
- 해결 방안 (어떤 기술로 해결했나?)
- 주요 기능 (스크린샷 포함)

## 🏗️ 아키텍처
- 시스템 아키텍처 다이어그램
- ERD (데이터베이스 스키마)
- 기술 스택 및 선택 이유

## 🔥 기술적 챌린지 & 해결
1. WebRTC 다중 참가자 동기화 이슈 → 해결 방법
2. 대용량 오디오 파일 처리 성능 개선 → S3 Presigned URL + 비동기 처리
3. AI 분석 병목 현상 해결 → RabbitMQ 큐잉 + 스케일링

## 📊 성능 지표
- 동시 접속자 처리 능력
- 평균 발음 분석 소요 시간
- API 응답 시간

## 🛠️ 로컬 실행 방법
(QUICK_START.md 통합)

## 🧪 테스트
- 테스트 커버리지 리포트
- 테스트 실행 방법

## 📦 배포
- CI/CD 파이프라인 설명
- 인프라 구성도
```

**작업:**
- [ ] 시스템 아키텍처 다이어그램 작성 (draw.io, Mermaid)
- [ ] ERD 생성 및 추가
- [ ] 주요 기능 GIF/스크린샷 추가
- [ ] 기술적 챌린지 섹션 작성 (면접 대비)
- [ ] 성능 벤치마크 결과 추가

---

### 1.2 API 문서 자동화

**현재 상태:**
- SpringDoc OpenAPI 의존성은 있으나 문서 활용도 불명

**개선 목표:**
```bash
# Swagger UI 접근
http://localhost:8080/swagger-ui/index.html

# API 스펙 JSON
http://localhost:8080/v3/api-docs
```

**작업:**
- [ ] 모든 Controller에 `@Tag`, `@Operation` 어노테이션 추가
- [ ] DTO에 `@Schema` 설명 추가
- [ ] 예시 Request/Response 추가 (`@io.swagger.v3.oas.annotations.media.ExampleObject`)
- [ ] FastAPI에도 자동 문서화 활성화 (`/docs`, `/redoc`)
- [ ] Postman Collection 생성 및 공유

---

### 1.3 아키텍처 문서 작성

**새 문서 생성:**
```
docs/
├── ARCHITECTURE.md          # 전체 시스템 설계
├── DATABASE_DESIGN.md       # ERD + 테이블 설명
├── API_DESIGN.md            # RESTful API 설계 원칙
├── MESSAGE_QUEUE_FLOW.md    # RabbitMQ 메시지 흐름도
└── DEPLOYMENT.md            # 배포 전략 및 인프라
```

**작업:**
- [ ] ARCHITECTURE.md: C4 Model 또는 Layered Architecture 다이어그램
- [ ] DATABASE_DESIGN.md: ERD + 인덱스 전략 설명
- [ ] MESSAGE_QUEUE_FLOW.md: 비동기 분석 플로우 시퀀스 다이어그램

---

## 2️⃣ **테스트 커버리지 향상** (필수 - 2주)

> **중요도: ★★★★★**
> 코드 품질과 유지보수성을 증명하는 핵심 지표.

### 2.1 FastAPI 단위 테스트 추가

**현재 상태:**
- app/test/에 수동 테스트 스크립트만 존재
- 실제 pytest 기반 테스트 없음

**목표:**
- 테스트 커버리지 80% 이상

**작업:**
```bash
# 새 디렉토리 구조
meari-ai/
├── app/
│   └── ...
└── tests/
    ├── __init__.py
    ├── conftest.py              # pytest fixtures
    ├── test_analysis_service.py # 발음 분석 로직 테스트
    ├── test_rabbitmq.py         # RabbitMQ 통합 테스트 (testcontainers)
    └── test_api.py              # FastAPI 엔드포인트 테스트
```

**구현 예시:**
```python
# tests/test_analysis_service.py
import pytest
from app.services.analysis_service import AnalysisService

@pytest.fixture
def analysis_service():
    return AnalysisService()

def test_pronunciation_accuracy_calculation(analysis_service):
    # Given
    reference_text = "안녕하세요"
    transcribed_text = "안녕하세요"

    # When
    accuracy = analysis_service.calculate_accuracy(reference_text, transcribed_text)

    # Then
    assert accuracy == 100.0

def test_mispronunciation_detection(analysis_service):
    # Given
    audio_file = "tests/fixtures/sample_audio.wav"
    reference = "테스트"

    # When
    errors = analysis_service.detect_errors(audio_file, reference)

    # Then
    assert len(errors) >= 0
    assert all(isinstance(e, dict) for e in errors)
```

**필요 도구:**
- pytest
- pytest-cov (커버리지 측정)
- pytest-asyncio (비동기 테스트)
- testcontainers-python (RabbitMQ 통합 테스트)

**작업:**
- [ ] pytest 환경 설정
- [ ] analysis_service 단위 테스트 (모델 mocking)
- [ ] RabbitMQ 통합 테스트 (testcontainers)
- [ ] API 엔드포인트 테스트
- [ ] 커버리지 리포트 생성 (`pytest --cov=app --cov-report=html`)

---

### 2.2 Spring Boot 테스트 확장

**현재 상태:**
- 17개 테스트 파일 존재
- 커버리지 측정 안 됨

**작업:**
- [ ] JaCoCo 플러그인 추가 (build.gradle)
```gradle
plugins {
    id 'jacoco'
}

jacoco {
    toolVersion = "0.8.11"
}

test {
    finalizedBy jacocoTestReport
}

jacocoTestReport {
    reports {
        xml.required = true
        html.required = true
    }
}
```
- [ ] 테스트 커버리지 80% 목표 달성
- [ ] 중요 비즈니스 로직 테스트 추가 (RoomService, AnalysisService 등)
- [ ] 통합 테스트 추가 (@SpringBootTest + Testcontainers)

---

### 2.3 E2E 테스트 (선택)

**시나리오:**
1. 사용자 회원가입 → 로그인
2. 연습방 생성 → 참가
3. 발음 녹음 → 분석 요청 → 결과 조회

**도구:**
- REST Assured (Spring Boot)
- Playwright 또는 Selenium (Frontend)

---

## 3️⃣ **모니터링 & 관찰성** (중요 - 2주)

> **중요도: ★★★★☆**
> 프로덕션 운영 경험을 보여주는 핵심 요소.

### 3.1 로깅 체계화

**현재 상태:**
- 기본 Spring Boot 로깅만 사용

**개선 목표:**

**Spring Boot:**
```java
// 구조화된 로깅 (Logback + Logstash Encoder)
@Slf4j
public class AnalysisService {

    public void analyzePronounciation(Long roomId, Long memberId) {
        log.info("발음 분석 시작 - roomId: {}, memberId: {}", roomId, memberId);

        try {
            // 비즈니스 로직
            log.debug("S3에서 오디오 다운로드 완료");
            log.info("발음 분석 완료 - roomId: {}, 소요시간: {}ms", roomId, duration);
        } catch (Exception e) {
            log.error("발음 분석 실패 - roomId: {}, memberId: {}", roomId, memberId, e);
            throw e;
        }
    }
}
```

**작업:**
- [ ] Logback 설정 파일 추가 (src/main/resources/logback-spring.xml)
```xml
<configuration>
    <appender name="JSON_FILE" class="ch.qos.logback.core.FileAppender">
        <file>logs/application.json</file>
        <encoder class="net.logstash.logback.encoder.LogstashEncoder"/>
    </appender>

    <root level="INFO">
        <appender-ref ref="JSON_FILE"/>
    </root>
</configuration>
```
- [ ] FastAPI 구조화된 로깅 (structlog)
- [ ] 에러 추적 시스템 (Sentry) 연동 (선택)

---

### 3.2 메트릭 수집 (Prometheus + Grafana)

**목표:**
- API 응답 시간 모니터링
- RabbitMQ 큐 길이 추적
- 발음 분석 처리 시간 측정
- DB 커넥션 풀 상태

**작업:**

**Spring Boot:**
```gradle
// build.gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

```yaml
# application.yml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

**FastAPI:**
```python
# requirements.txt
prometheus-client
prometheus-fastapi-instrumentator

# main.py
from prometheus_fastapi_instrumentator import Instrumentator

app = FastAPI()
Instrumentator().instrument(app).expose(app)
```

**docker-compose.yml 추가:**
```yaml
prometheus:
  image: prom/prometheus:latest
  volumes:
    - ./prometheus.yml:/etc/prometheus/prometheus.yml
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana:latest
  ports:
    - "3001:3000"
  environment:
    - GF_SECURITY_ADMIN_PASSWORD=admin
```

**작업:**
- [ ] Actuator + Prometheus 설정
- [ ] Prometheus 설정 파일 작성
- [ ] Grafana 대시보드 생성
  - Spring Boot Metrics 대시보드
  - RabbitMQ 대시보드
  - PostgreSQL 대시보드

---

### 3.3 Health Check 강화

**현재:**
- 기본 헬스체크만 존재

**개선:**
```java
@Component
public class RabbitMQHealthIndicator implements HealthIndicator {

    @Override
    public Health health() {
        try {
            // RabbitMQ 연결 테스트
            return Health.up()
                .withDetail("queues", queueStatus)
                .build();
        } catch (Exception e) {
            return Health.down()
                .withDetail("error", e.getMessage())
                .build();
        }
    }
}
```

**작업:**
- [ ] Custom HealthIndicator 추가 (RabbitMQ, S3, FastAPI 연결)
- [ ] Readiness/Liveness Probe 분리 (K8s 대비)

---

## 4️⃣ **성능 최적화** (중요 - 1.5주)

> **중요도: ★★★★☆**
> 대규모 트래픽 처리 능력을 증명.

### 4.1 부하 테스트

**도구:**
- JMeter 또는 k6

**시나리오:**
1. 동시 사용자 100명 로그인
2. 50개 방 동시 생성
3. 200개 발음 분석 요청 동시 처리

**작업:**
- [ ] k6 스크립트 작성
```javascript
// load-test.js
import http from 'k6/http';
import { check } from 'k6';

export let options = {
  stages: [
    { duration: '1m', target: 50 },
    { duration: '3m', target: 100 },
    { duration: '1m', target: 0 },
  ],
};

export default function () {
  let res = http.get('http://localhost:8080/api/v1/rooms');
  check(res, { 'status was 200': (r) => r.status == 200 });
}
```
- [ ] 테스트 결과 문서화 (처리량, 응답 시간, 에러율)
- [ ] 병목 구간 분석 (Prometheus 메트릭 활용)

---

### 4.2 데이터베이스 최적화

**작업:**
- [ ] 쿼리 성능 분석 (slow query log 활성화)
- [ ] N+1 문제 해결 (EntityGraph, Fetch Join)
- [ ] 인덱스 최적화
```sql
-- 예: 자주 조회되는 컬럼에 인덱스 추가
CREATE INDEX idx_room_member_id ON room_member(member_id);
CREATE INDEX idx_shadowing_report_room_id ON shadowing_report(room_id);
```
- [ ] 커넥션 풀 튜닝 (HikariCP)
```yaml
spring:
  datasource:
    hikari:
      maximum-pool-size: 20
      minimum-idle: 10
      connection-timeout: 30000
```

---

### 4.3 캐싱 전략 강화

**현재:**
- Redis 세션 저장소로만 사용

**개선:**
```java
@Cacheable(value = "content", key = "#contentId")
public ContentDto getContent(Long contentId) {
    return contentRepository.findById(contentId)...
}

@CacheEvict(value = "content", key = "#contentId")
public void updateContent(Long contentId, ...) {
    ...
}
```

**작업:**
- [ ] 자주 조회되는 데이터 캐싱 (콘텐츠, 대시보드 통계)
- [ ] 캐시 만료 정책 설정
- [ ] 캐시 히트율 모니터링

---

## 5️⃣ **보안 강화** (중요 - 1주)

> **중요도: ★★★★☆**
> 실무 수준의 보안 인식을 보여줌.

### 5.1 보안 점검 체크리스트

**작업:**
- [ ] OWASP Top 10 점검
  - SQL Injection 방지 (JPA 사용으로 대부분 해결됨)
  - XSS 방지 (입력 값 검증)
  - CSRF 방지 (Spring Security 기본 설정 확인)
- [ ] 민감 정보 노출 방지
  - application.yml에 비밀번호 하드코딩 제거 (환경변수 사용)
  - 에러 메시지에 스택 트레이스 노출 방지
- [ ] Rate Limiting 추가 (Bucket4j)
```java
@RateLimiter(name = "api")
public ResponseEntity<?> createRoom(...) {
    ...
}
```
- [ ] HTTPS 강제 (Nginx 리다이렉트)
- [ ] CORS 정책 재검토

---

### 5.2 의존성 취약점 스캔

**작업:**
- [ ] Gradle Dependency Check 플러그인 추가
```gradle
plugins {
    id 'org.owasp.dependencycheck' version '8.4.0'
}
```
- [ ] pip-audit로 Python 의존성 검사
```bash
pip install pip-audit
pip-audit
```

---

## 6️⃣ **코드 품질 & Best Practices** (선택 - 1주)

### 6.1 정적 분석 도구 도입

**도구:**
- SonarQube (코드 품질 분석)
- CheckStyle (Java 코딩 컨벤션)
- Black + Flake8 (Python)

**작업:**
- [ ] SonarQube 로컬 실행
```yaml
# docker-compose.yml
sonarqube:
  image: sonarqube:community
  ports:
    - "9000:9000"
```
- [ ] Gradle에 SonarQube 플러그인 추가
- [ ] 코드 스멜 제거 (중복 코드, 복잡도 높은 메서드)

---

### 6.2 성능 프로파일링

**작업:**
- [ ] Spring Boot Actuator HTTP Trace 활성화
- [ ] JVM 프로파일링 (VisualVM 또는 YourKit)
- [ ] 메모리 누수 검사

---

## 7️⃣ **추가 기능 (선택 - 면접 시 언급용)**

### 7.1 블루-그린 배포

**작업:**
- [ ] Jenkins 파이프라인 개선
- [ ] 무중단 배포 스크립트 작성

---

### 7.2 쿠버네티스 전환 (선택)

**작업:**
- [ ] Kubernetes 매니페스트 작성
```yaml
# k8s/deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: meari-backend
spec:
  replicas: 3
  ...
```
- [ ] Helm Chart 작성

---

### 7.3 메시지 큐 확장성 개선

**작업:**
- [ ] Dead Letter Queue 설정
- [ ] 재시도 정책 추가
- [ ] Priority Queue 도입 (프리미엄 사용자 우선 처리)

---

## 📋 우선순위 요약

### 필수 (2주 안에 완료)
1. ✅ README.md 개선 (아키텍처 다이어그램 포함)
2. ✅ FastAPI 테스트 추가 (커버리지 80%)
3. ✅ API 문서화 (Swagger 보강)
4. ✅ Prometheus + Grafana 모니터링

### 중요 (1달 안에)
5. ✅ 부하 테스트 실행 및 결과 문서화
6. ✅ 데이터베이스 쿼리 최적화
7. ✅ 보안 점검 체크리스트
8. ✅ JaCoCo 커버리지 리포트

### 선택 (여유 있을 때)
9. ⭕ SonarQube 코드 품질 분석
10. ⭕ Kubernetes 배포 (이력서에 K8s 경험 추가 가능)
11. ⭕ E2E 테스트

---

## 💼 이력서/포트폴리오 작성 팁

### 프로젝트 설명 예시
```
[Meari - AI 기반 한국어 발음 교정 플랫폼]

■ 프로젝트 개요
- WebRTC 기반 실시간 화상 연습 + AI 발음 분석 SaaS
- 4인 팀 프로젝트, 백엔드 개발 담당

■ 담당 역할
- Spring Boot 기반 RESTful API 설계 및 개발 (18개 도메인)
- RabbitMQ를 활용한 비동기 발음 분석 파이프라인 구축
- Jenkins + Docker 기반 CI/CD 파이프라인 구축
- PostgreSQL 스키마 설계 및 쿼리 최적화

■ 기술적 성과
- RabbitMQ 도입으로 발음 분석 처리 속도 3배 향상 (동기 → 비동기)
- 쿼리 최적화 및 인덱스 설계로 대시보드 API 응답 시간 80% 감소 (2s → 400ms)
- JaCoCo 테스트 커버리지 85% 달성 (단위 테스트 120개)
- Prometheus + Grafana 모니터링으로 장애 감지 시간 90% 단축

■ 기술 스택
Backend: Java 21, Spring Boot 3.5, JPA, Spring Security
Database: PostgreSQL, Redis
Message Queue: RabbitMQ
AI: FastAPI, Wav2Vec2, PyTorch
Infra: Docker, Jenkins, AWS S3, Nginx
```

### 면접 예상 질문 대비
1. **RabbitMQ를 왜 선택했나요?**
   - "실시간 발음 분석은 최대 30초가 걸리는데, 동기 처리 시 사용자가 대기해야 합니다. RabbitMQ로 비동기 처리하여 즉시 응답하고, 분석 완료 시 WebSocket으로 결과를 푸시합니다."

2. **대용량 트래픽 처리 경험은?**
   - "k6로 100명 동시 접속 시나리오를 테스트했고, Prometheus 메트릭으로 DB 커넥션 풀 부족 문제를 발견해 HikariCP 설정을 튜닝했습니다."

3. **장애 대응 경험은?**
   - "Grafana 알람으로 RabbitMQ 큐가 1000개 이상 쌓인 것을 감지했고, FastAPI 워커를 2개에서 4개로 스케일링하여 해결했습니다."

---

## 🎓 학습 자료 추천

### 책
- 『가상 면접 사례로 배우는 대규모 시스템 설계 기초』
- 『Effective Java 3/E』
- 『Real MySQL 8.0』

### 강의
- 인프런: 『스프링 부트 - 핵심 원리와 활용』(김영한)
- 패스트캠퍼스: 『대용량 트래픽 처리』

### 블로그 시리즈
- 우아한형제들 기술블로그: Redis 활용 사례
- 카카오 기술블로그: RabbitMQ 운영 경험

---

## ✅ 체크리스트

### 1주차
- [ ] README.md 아키텍처 다이어그램 추가
- [ ] ERD 생성 및 문서화
- [ ] Swagger 어노테이션 전체 추가
- [ ] FastAPI pytest 환경 설정

### 2주차
- [ ] FastAPI 단위 테스트 80% 커버리지
- [ ] JaCoCo 플러그인 추가 및 리포트 생성
- [ ] Prometheus + Grafana 설정

### 3주차
- [ ] k6 부하 테스트 실행
- [ ] 데이터베이스 인덱스 최적화
- [ ] 보안 점검 (OWASP Top 10)

### 4주차
- [ ] SonarQube 코드 품질 분석
- [ ] 캐싱 전략 적용
- [ ] 최종 문서 정리

---

## 📞 다음 단계

위 계획 중 어떤 부분부터 시작하고 싶으신가요?

**추천 순서:**
1. **문서화** (1주) - 빠르게 포트폴리오 퀄리티 향상
2. **테스트** (1주) - 코드 품질 증명
3. **모니터링** (1주) - 운영 경험 강조
4. **성능 최적화** (1주) - 기술적 깊이 증명

각 단계마다 제가 구체적으로 도와드릴 수 있습니다!
