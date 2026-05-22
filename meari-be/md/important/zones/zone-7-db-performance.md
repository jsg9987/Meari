# Zone 7 — DB 성능 (N+1 / 쿼리 폭발 / 페이지네이션) 실행 계획

> 마스터 플랜의 Z7 상세본. 측정이 가장 명확하고 Before/After가 시각적이라 포폴 가시성 높음.

## 목표

다음 5개 명제를 면접에서 1인칭으로 답할 수 있게 만들기:

1. **SQL 로그 켜고 실제 쿼리 수를 카운트** — 이론 N+1이 아니라 측정된 N+1
2. **`getRoomDetail` 류**에서 lazy 컬렉션 접근으로 발생하는 N+1을 fetch join으로 1쿼리화
3. **Report/Dashboard 리스트** 조회의 쿼리 폭발 점검
4. **JSONB 파싱 비용** — `shadowing_report` JSONB 컬럼 deserialization 시간 측정 (정규화 대안 비교)
5. **인덱스 누락** — 자주 쓰는 WHERE/JOIN 컬럼에 인덱스가 있는지 `EXPLAIN ANALYZE`로 확인

## 현재 코드 상태 (Pre-Z7 베이스라인)

### 이미 잘 된 곳 (Pre-existing fetch join)

| 위치 | 상태 |
|---|---|
| `RoomRepository.findRoomsWithCursor` | ✅ `JOIN FETCH r.theme, r.owner` |
| `RoomRepository.findAllRoomsWithCursor` | ✅ 동일 |
| `MemberRoomRepository.findByRoomIdWithMember` | ✅ `JOIN FETCH mr.member` |
| `MemberRoomRepository.findFirstByRoomIdOrderByCreatedAtAsc` | ✅ 동일 |
| `MemberRoomRepository.findByMemberIdWithRoom` | ✅ `JOIN FETCH mr.room` |

→ 커서 페이지네이션은 잘 되어 있음. **Z7의 발굴은 이 외 영역**에서.

### N+1 후보 위치

| 파일/위치 | 라인 | 문제 | 측정 방법 |
|---|---|---|---|
| `RoomService.getRoomDetail()` | (확인 필요) | `Room` 조회 후 `room.getOwner()` lazy 접근 + `memberRoomRepository.findByRoomIdWithMember(...)` 별도 호출 + content/script 접근 | SQL 로그 카운트 |
| `RoomService.leaveRoom()` `RoomService.kickMember()` 등 | 372 등 | `room.getOwner().getMemberId()` lazy 접근 — 매번 owner 조회 SELECT 1번 추가 | 같음 |
| Report 리스트 (`ShadowingReport`, `KopicReport`) | (확인 필요) | 리스트 조회 시 멤버/콘텐츠 lazy | 같음 |
| Dashboard (`DashboardServiceImpl`) | 249L | 집계 쿼리 — JPQL `GROUP BY` 또는 native? | `EXPLAIN ANALYZE` |
| solo_practice 관련 | 220L | 1:1 연습 시작 시 어떤 조회가? | SQL 로그 |
| 분석 결과 응답 (`ShadowingReportService` JSONB) | 170L | JSONB → DTO 매핑 시간 | 처리 시간 측정 |

### Entity LAZY 매핑 현황

| Entity | 필드 | 매핑 |
|---|---|---|
| `Room` | `theme`, `owner` | `@ManyToOne(LAZY)` |
| `MemberRoom` | `room`, `member` | `@ManyToOne(LAZY)` |

→ 이걸 access할 때마다 1 SELECT. fetch join 없는 경로에서는 거의 확실히 N+1.

---

## Pre-flight 체크리스트

- [ ] `application.yml`에 SQL 로그 켜기:
  ```yaml
  spring:
    jpa:
      show-sql: true
      properties:
        hibernate:
          format_sql: true
          use_sql_comments: true
  logging:
    level:
      org.hibernate.SQL: DEBUG
      org.hibernate.orm.jdbc.bind: TRACE  # 바인드 파라미터까지 보고 싶을 때
  ```
- [ ] (선택) `p6spy` 의존성 추가 — 실제 쿼리 + 시간 측정용
  ```gradle
  runtimeOnly 'com.github.gavlyukovskiy:p6spy-spring-boot-starter:1.9.0'
  ```
- [ ] 데이터 시드 가능 — 방 100개, 방마다 4명, 라운드 2회씩 등
  - SQL 직접 실행 또는 BE에 `CommandLineRunner`로 시드
- [ ] DB 도구로 `EXPLAIN ANALYZE` 실행 가능 (psql, DBeaver, pgAdmin)
- [ ] BE 응답 시간 측정 가능 (Postman 응답 시간, `curl -w "%{time_total}"`)

---

## 시나리오 — 직접 수행 순서

각 시나리오 결과를 `meari-be/md/_db_performance_ts_raw.md`에 누적.

### SC1. `getRoomList` 베이스라인 (이미 fetch join — 음성 대조군)
- [ ] Hypothesis: 이미 fetch join 적용된 경로는 N+1 없음 (1쿼리)
- Setup: 데이터 시드 — 방 100개
- Execute:
  1. `GET /api/v1/rooms?cursor=&limit=20` 호출
  2. SQL 로그에서 SELECT 카운트
  3. 응답 시간 기록
- Observe: 1쿼리 + 카운트 쿼리(혹은 0) — 정상이면 1~2개
- Conclude: 잘 된 패턴의 베이스라인. 이후 SC2와 비교 기준

### SC2. `getRoomDetail` N+1 발굴
- [ ] Hypothesis: 방 상세 조회는 Room + Owner + Members + Content + Roles 등 여러 lazy 컬렉션을 접근하므로 1+N+M+... 쿼리 발생
- Setup: 시드 — 방 1개에 멤버 4명, 콘텐츠 1개, 역할 4개
- Execute:
  1. `GET /api/v1/rooms/{roomId}` 호출
  2. SQL 로그에서 SELECT 카운트 + 어느 라인에서 발생하는지 매핑
  3. 응답 시간 기록
- Observe: 예상 — 1(Room) + 1(Owner) + N(Members fetch join 있어도 추가 lazy) + 1(Content) + N(Roles) ≈ 5~10
- Conclude: 정량 카운트 → fetch join 추가 효과의 근거

### SC3. Report 리스트 N+1
- [ ] Hypothesis: `ShadowingReport` 리스트 조회 시 연결된 회원/콘텐츠 lazy 접근
- Setup: 한 회원이 보유한 ShadowingReport 50개
- Execute:
  1. mypage의 리포트 목록 API 호출 (정확한 endpoint 확인)
  2. SQL 카운트
  3. JSONB 컬럼 deserialization 시간 추정 (응답 시간 - SQL 시간)
- Observe: 1+50=51 vs 1
- Conclude: Report 리스트의 N+1 명확화

### SC4. Dashboard 집계 쿼리
- [ ] Hypothesis: 일별/주별 통계 쿼리는 GROUP BY 활용으로 1쿼리지만, `EXPLAIN ANALYZE`로 보면 인덱스 부재로 Seq Scan 가능
- Setup: 시드 — 한 회원 30일치 daily_record + 30일치 ShadowingReport
- Execute:
  1. `GET /api/v1/dashboard/...` 호출
  2. SQL 로그에서 발급 쿼리 추출
  3. 그 쿼리를 `EXPLAIN ANALYZE`로 실행 — Seq Scan vs Index Scan
- Observe: `created_at`, `member_id` 인덱스 사용 여부
- Conclude: 인덱스 추가 후보 식별

### SC5. JSONB 파싱 비용
- [ ] Hypothesis: ShadowingReport.detailedAnalysis JSONB 컬럼이 큰 경우(10문장×문장당 분석) deserialization이 응답 시간의 절반 이상
- Setup: 분석 결과 풍부한 Report 1건 (10문장)
- Execute:
  1. 단순 카운트: `SELECT COUNT(*) FROM shadowing_report WHERE id = X` 의 시간
  2. 전체 조회: 같은 행 SELECT * → JSONB 포함
  3. BE에서 그 행 조회 + DTO 변환 → 응답까지 시간
  4. 차이 계산: (3) - (2)가 deserialization 비용
- Observe: 병목이 SQL인지 매핑인지
- Conclude: JSONB 비용 정량화 → 부분 조회 또는 캐시 도입의 근거

### SC6. 100배 데이터 부하
- [ ] Hypothesis: 작은 데이터에서는 안 보이던 N+1이 100배 데이터에서는 응답 시간 폭증
- Setup: SC2 시드의 100배 (방 100개, 방당 4명)
- Execute:
  1. `getRoomList` 응답 시간 (SC1 결과와 비교)
  2. `getRoomDetail` 응답 시간 (SC2 결과와 비교)
  3. p50/p95 측정
- Observe: 데이터 1배 vs 100배에서 응답 시간 비례인지 비선형인지
- Conclude: N+1의 실제 영향이 데이터 크기 따라 어떻게 변하는지

---

## 발견 후 수정 계획 (PR 분할)

### PR1: `getRoomDetail`에 `@EntityGraph` 또는 fetch join 추가
- 현재 구조 분석 후 다음 중 선택:
  - 옵션 A: `RoomRepository.findByIdWithDetail(Long roomId)` — `JOIN FETCH r.theme, r.owner, r.content, r.content.scripts`
  - 옵션 B: `@EntityGraph(attributePaths = {"theme", "owner", "content"})` 어노테이션
- Service에서 `findById` 대신 새 메서드 사용
- 검증: SC2 재현 시 쿼리 1~2개로 감소

### PR2: lazy `room.getOwner()` 접근 패턴 정리
- `RoomService` 곳곳에서 `room.getOwner().getMemberId()` 호출 → owner 한 번만 fetch
- 옵션: 자주 쓰는 경로(`leaveRoom`, `kickMember`)의 `findById`를 fetch join 메서드로 교체
- 검증: 해당 경로 SQL 카운트 -1

### PR3: Report 리스트 fetch join 또는 Projection
- 큰 엔티티 통째로 안 가져오고 필요 필드만 SELECT:
  ```java
  @Query("SELECT new com.ssafy.meari.domain.report.dto.ReportSummary(...) FROM ShadowingReport sr WHERE ...")
  ```
- 또는 `@EntityGraph`로 fetch join
- 검증: SC3 재현 시 1쿼리

### PR4: Dashboard 인덱스 추가
- `EXPLAIN ANALYZE` 결과에 따라 누락된 인덱스 식별:
  - `daily_record(member_id, created_at)`
  - `shadowing_report(member_id, created_at)`
- Liquibase/Flyway 마이그레이션으로 추가 (또는 DDL 직접)
- 검증: SC4 재실행 시 Index Scan으로 변경

### PR5: JSONB 부분 조회 또는 lazy
- 옵션 A: 리스트 조회 시 JSONB 제외 — `SELECT id, member_id, accuracy, intonation, confidence FROM shadowing_report ...` (부분 컬럼)
- 옵션 B: 상세 조회에서만 JSONB 가져오게 분리
- 옵션 C: 자주 쓰이는 필드는 정규화하고, raw JSON만 캐시
- 검증: SC5의 deserialization 비용 감소

### PR6: SQL 로그 운영 모드 분리
- `application-dev.yml`: SQL show=true, format=true (개발 편의)
- `application-prod.yml`: SQL show=false (성능 + 정보 노출 방지)
- p6spy는 dev 프로파일에서만 활성화

---

## 측정 지표 표 (Before/After 채우기)

| 지표 | Before | After (목표) |
|---|---|---|
| `getRoomList` 쿼리 수 (SC1) | (측정) | 변화 없음 (이미 잘 됨) |
| `getRoomDetail` 쿼리 수 (SC2) | (측정 5~10?) | 1~2 |
| `getRoomDetail` 응답 시간 p95 | (측정 ms) | -50%+ |
| Report 리스트 쿼리 수 (SC3) | 1+N | 1 |
| Dashboard 쿼리 plan (SC4) | Seq Scan | Index Scan |
| Dashboard 쿼리 시간 | (측정) | -50%+ |
| ShadowingReport 응답 시간 (JSONB) (SC5) | (측정) | -30% (부분 조회 시) |
| 100배 데이터 `getRoomDetail` 시간 (SC6) | (측정) | 1배 데이터 대비 ±10% |

---

## 정리 산출물

- `meari-be/md/_db_performance_ts_raw.md` — 시나리오별 raw + SQL 로그 발췌
- `meari-be/md/portfolio_features.md` 신규 또는 #8(커서 페이지네이션)에 보강:
  - "쿼리 카운트로 측정한 N+1 — fetch join으로 X→Y, 응답시간 N→M"
  - "EXPLAIN ANALYZE로 발견한 Seq Scan → 복합 인덱스 추가 → P→Q"
  - "JSONB 컬럼 비용 측정 후 부분 조회 패턴 적용"

---

## 진행 체크리스트

- [ ] Pre-flight (SQL 로그 + p6spy + 시드)
- [ ] SC1 `getRoomList` 베이스라인
- [ ] SC2 `getRoomDetail` N+1
- [ ] SC3 Report 리스트
- [ ] SC4 Dashboard 인덱스
- [ ] SC5 JSONB 비용
- [ ] SC6 100배 데이터
- [ ] PR1 getRoomDetail fetch join
- [ ] PR2 lazy 접근 정리
- [ ] PR3 Report Projection
- [ ] PR4 인덱스 추가
- [ ] PR5 JSONB 부분 조회
- [ ] PR6 운영 모드 SQL 로그 분리
- [ ] 측정 지표 표 채우기
- [ ] portfolio 갱신

---

**작성일**: 2026-04-29
**다음 액션**: SQL 로그 활성화 → 시드 생성 → SC1 베이스라인
