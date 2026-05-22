# Zone 1 — 인증·세션 (BE 전용) 실행 계획

> 마스터 플랜의 Z1 상세본. FE 부분 제외, BE 인증·인가·토큰 수명주기·검증 영역 종합 점검.

## 목표

다음 9개 명제를 면접에서 1인칭으로 답할 수 있게 만들기:

1. **약한 비밀번호 통과** — `"a"`, `"123"` 같은 1자리/단순 비밀번호로 가입 가능. `@Pattern` 도입으로 0 → 100% 차단
2. **음수/잘못된 PathVariable** — `roomId = -999` 같은 입력이 어디까지 흘러가는지 확인 후 `@Positive`로 차단
3. **SecurityConfig permitAll 매트릭스** — `/api/v1/openvidu/**`, `/api/v1/rooms/**`, `/api/v1/contents/**` 가 정말 인증 없이 통과되는지 매트릭스로 입증
4. **JwtUtil lazy init thread-safety** — `cachedSecretKey == null` 체크가 atomic 아님. 멀티 스레드 부하로 race 검증 후 `@PostConstruct`로 일괄 초기화
5. **만료 AT logout edge case** — `TokenBlacklistService.addToBlacklist`가 `getRemainingTime` 호출 → ExpiredJwtException 가능성. 실제 도달 가능한지 확인
6. **다중 디바이스 동시 로그인** — Refresh Token이 `email` 단일 키 → 두 번째 로그인이 첫 번째 RT를 덮어쓰는지 실측. 의도된 정책인지 결정
7. **AuthService.refreshAccessToken broad catch** — `BusinessException` 외 모든 예외를 `INVALID_REFRESH_TOKEN`으로 뭉뚱그려 RT 만료 사유가 가려짐. ExpiredJwt 별도 처리
8. **dead route `/api/v1/members/login`** — SecurityConfig에 permitAll 있는데 매칭 컨트롤러 없음. 직접 호출 시 어떤 응답인지 확인 후 정리
9. **테스트 페이지 노출** — `/webrtc-test.html`, `/websocket-test.html` 운영에 노출되면 안 됨

## 현재 코드 상태 (Pre-Z1 베이스라인)

### 핵심 위치

| 파일 | 라인 | 책임 | 위험 |
|---|---|---|---|
| `SecurityConfig.java` | 100~108 | "테스트용" permitAll 블록 — `/api/v1/openvidu/**`, `/api/v1/rooms/**`, `/api/v1/contents/**` | **운영 보안 구멍** |
| `SecurityConfig.java` | 80~86 | 주석 처리된 정적 파일 permitAll (chat-test 잔재) | 가독성 |
| `SecurityConfig.java` | 98 | `webrtc-test.html` permitAll | 운영 노출 |
| `SecurityConfig.java` | 111 | `/api/v1/members/login`, `/api/v1/members/signup` permitAll | dead route 가능성 |
| `JwtUtil.java` | 31~43 | `cachedSecretKey` lazy init — null 체크 + `new SecretKeySpec(...)` 비원자 | 멀티스레드 race |
| `JwtUtil.java` | 45~46 | `// TODO 멀티스레드 환경에서 synchronized 처리 고려` | 명시된 미해결 |
| `SignupRequestDto.java` | 10 | TODO 비밀번호 패턴 검증 누락 | 약한 비밀번호 통과 |
| `PasswordUpdateRequestDto.java` | 5 | 동일 | 동일 |
| Controller PathVariable 전반 | — | `@Positive`/`@Min` 부재, `@Validated` 누락 | 음수/0 통과 |
| `AuthService.java` | 56~61 | `refreshAccessToken` catch (BusinessException 외 모두 INVALID_REFRESH_TOKEN) | 사유 가려짐 |
| `AuthService.java` | 65~73 | `logout(email, accessToken)` — `TokenBlacklistService.addToBlacklist` 호출 | 만료 토큰 들어오면? |
| `TokenBlacklistService.java` | 19~32 | `addToBlacklist` → `jwtUtil.getRemainingTime(token)` → `validateToken` → `ExpiredJwtException` 가능 | logout 시 500? |
| `RefreshTokenService.java` | 21~28 | `redisTemplate.set("refresh:" + email, ...)` | 이메일 단일 키 — 다중 디바이스 미지원 |

### Section 1A에서 이미 본 추가 의문점
- `WebSecurityCustomizer` ignoring + `authorizeHttpRequests` permitAll 중복 (Swagger)
- `/api/v1/auth/**` permitAll인데 logout/탈퇴는 사실상 토큰 필요 — JwtAuthenticationFilter가 막아서 결과적 안전이지만 책임 경계 모호

---

## Pre-flight 체크리스트

- [ ] BE 로컬 실행 가능 (직접 컨트롤)
- [ ] Postman 또는 curl 사용 가능
- [ ] BE 로그 실시간 확인 (예외 트레이스 캡처용)
- [ ] DB 도구 (`member` 테이블 직접 확인)
- [ ] Redis CLI (`refresh:*` 키 직접 확인)
- [ ] (SC4용) k6 또는 짧은 스크립트로 100 RPS 발사 가능
- [ ] 테스트용 회원 2~3개 (멀티 디바이스 시뮬용)
- [ ] application.yml의 토큰 만료 시간을 임시로 짧게(예: AT 30초)로 줄여 만료 시나리오 빠르게 재현 가능 — 끝나면 원복

---

## 시나리오 — 직접 수행 순서

각 시나리오 결과를 `meari-be/md/_auth_session_ts_raw.md`에 누적.

### SC1. 약한 비밀번호 가입
- [ ] Hypothesis: `password = "a"`, `"123"`, `"aaaa"` 같은 약한 값이 통과
- Setup: 회원가입 엔드포인트 `POST /api/v1/auth/signup`
- Execute:
  1. 다음 비밀번호로 각각 가입 시도:
     - `"a"`, `"1"`, `"12345"`, `"aaaaaaaa"`, `"password"`, `"123456789"`
  2. 어떤 게 통과/거부되는지 매트릭스 작성
  3. DB에서 BCrypt 해시 확인 (성공한 케이스)
- Observe: 통과율, 거부 사유
- Conclude: 비밀번호 정책 부재의 정량 증거

### SC2. PathVariable 음수/잘못된 값
- [ ] Hypothesis: 음수 roomId, 0, 매우 큰 값이 어디서 어떻게 처리되는가
- Setup: 인증된 사용자
- Execute:
  1. `GET /api/v1/rooms/-999` 호출 → 응답 (404? 400? 500?)
  2. `GET /api/v1/rooms/0` 호출 → 응답
  3. `GET /api/v1/rooms/9999999999` (Long 범위 초과)
  4. `POST /api/v1/rooms/-1/enter` 등 다른 엔드포인트도 동일
  5. 응답 코드와 메시지 매트릭스
- Observe: GlobalExceptionHandler가 어떻게 처리하는지 (NOT_FOUND? TypeMismatch?)
- Conclude: 입력 검증의 깊이별 흐름 확인 — `@Positive` 도입 후 즉시 400 반환되는 게 정석

### SC3. SecurityConfig permitAll 매트릭스 ⭐
- [ ] Hypothesis: `/api/v1/openvidu/**`, `/api/v1/rooms/**`, `/api/v1/contents/**`가 토큰 없이 통과
- Setup: 토큰 없는 상태
- Execute:
  1. **토큰 없이** 다음 엔드포인트 호출:
     - `GET /api/v1/rooms` (방 목록)
     - `POST /api/v1/rooms` (방 생성 — 인증 필요한 행위)
     - `GET /api/v1/contents` (콘텐츠 목록)
     - `GET /api/v1/openvidu/sessions` (OpenVidu)
     - `POST /api/v1/openvidu/sessions` (세션 생성)
  2. 각각 응답 코드/메시지 기록
  3. 응답이 200/201이면 → **인증 우회 매트릭스** 항목으로 기록
  4. 응답이 401/403이면 → 어디서 막혔는지 (SecurityConfig vs Controller `@AuthenticationPrincipal` NPE vs 다른 필터)
- Observe: 어떤 엔드포인트가 진짜 인증 없이 작동하는지
- Conclude: **운영 진입 직전 보안 매트릭스** — 가장 강력한 포폴 자료

### SC4. JwtUtil lazy init multi-thread race
- [ ] Hypothesis: `cachedSecretKey` 초기화가 100 RPS 동시 발사 시 race condition 노출
- Setup: BE 재기동 직후 (cachedSecretKey == null 상태)
- Execute:
  1. JVM debugger 또는 코드에 `log.info("create cachedSecretKey: {}", System.identityHashCode(cachedSecretKey));` 추가
  2. k6 또는 스크립트로 인증 필요한 엔드포인트 동시 100 요청
  3. 로그에서 `create cachedSecretKey` 발생 횟수 확인 — 1회면 OK, 2회 이상이면 race
  4. 동시성을 더 강하게: 200, 500 RPS도 시도
- Observe: 실제 multiple init 발생 빈도
- Conclude: 이론 race가 실제로 표출되는지 정량화

### SC5. 만료 AT로 logout 호출
- [ ] Hypothesis: 만료된 AT로 logout 호출하면 `TokenBlacklistService.addToBlacklist` 안의 `getRemainingTime` → `validateToken` → `ExpiredJwtException` → 500
- Setup: AT 만료 시간 30초로 줄여둠
- Execute:
  1. 로그인 → AT 받음
  2. 31초 대기 (또는 토큰을 의도적으로 만료시킴)
  3. `POST /api/v1/auth/logout` with that AT
  4. 응답 확인
- Observe: 200? 500? 401?
- Conclude: 실제로는 `JwtAuthenticationFilter`가 먼저 막을 가능성 높음 — 흐름이 어디서 끊기는지 추적

### SC6. 다중 디바이스 동시 로그인
- [ ] Hypothesis: 같은 회원이 두 기기에서 로그인 → 두 번째 로그인이 첫 번째 RT를 덮어씀 → 첫 기기 refresh 시 무효
- Setup: 동일 회원 A. 브라우저 두 개 또는 Postman 두 워크스페이스
- Execute:
  1. 기기1에서 로그인 → AT1, RT1 받음. Redis 확인 → `refresh:A` = RT1
  2. 기기2에서 같은 회원 로그인 → AT2, RT2. Redis → `refresh:A` = RT2 (RT1 사라짐)
  3. 기기1에서 RT1으로 `/api/v1/auth/refresh` 호출 → 응답 (실패 예상)
  4. 기기2에서 RT2로 refresh → 정상
- Observe: 첫 기기 강제 로그아웃 효과 발생
- Conclude: 의도적이면 OK, 의도 아니면 키 패턴 변경 (`refresh:{email}:{deviceId}`)

### SC7. AuthService.refreshAccessToken broad catch
- [ ] Hypothesis: 만료된 RT로 refresh 호출 시 사용자에게 "RT 만료" 메시지 대신 "유효하지 않은 RT" 메시지가 감
- Setup: RT 만료 시간을 짧게 + RT 발급 후 만료 대기
- Execute:
  1. RT 만료된 후 `/api/v1/auth/refresh` 호출
  2. 응답 메시지 확인 — "Refresh Token이 만료되었습니다"인지 "유효하지 않은 Refresh Token입니다"인지
  3. AuthService 코드(line 56~61) 추적: ExpiredJwtException은 일반 Exception catch에 잡혀 INVALID_REFRESH_TOKEN으로 변환됨
- Observe: 사용자 노출 메시지 vs 실제 사유
- Conclude: 사유 구분 부재 — 사용자 경험 문제

### SC8. dead route `/api/v1/members/login`
- [ ] Hypothesis: SecurityConfig에 permitAll 있지만 매칭 컨트롤러 없음
- Setup: 토큰 없음
- Execute:
  1. `POST /api/v1/members/login` with `{"email":"...","password":"..."}` 호출
  2. 응답 — 404? 405? 500?
  3. 같은 페이로드로 `POST /api/v1/auth/login` (정상 경로) 비교
- Observe: dead route가 실제로 존재하는지
- Conclude: SecurityConfig 정리 대상

### SC9. 테스트 페이지 노출
- [ ] Hypothesis: `/webrtc-test.html`, `/websocket-test.html`가 운영에 노출
- Setup: 토큰 없음
- Execute:
  1. `GET /webrtc-test.html` — 응답
  2. `GET /websocket-test.html` — 응답
  3. 페이지 내용 확인 — 어떤 정보 노출되는지
- Observe: 실제 페이지 노출 여부
- Conclude: 운영 차단 필요성

---

## 발견 후 수정 계획 (PR 분할)

### PR1: 비밀번호 패턴 검증
- `SignupRequestDto.java`, `PasswordUpdateRequestDto.java`:
  ```java
  @NotBlank
  @Pattern(
      regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
      message = "비밀번호는 8자 이상이어야 하며 영문/숫자/특수문자를 모두 포함해야 합니다."
  )
  private String password;
  ```
- 검증: SC1 재현 시 약한 비밀번호 100% 거부

### PR2: PathVariable 검증
- 컨트롤러 클래스에 `@Validated` 추가 + 메서드 파라미터에 `@Positive`/`@Min(1)`:
  ```java
  @GetMapping("/{roomId}")
  public ResponseEntity<...> getRoom(@PathVariable @Positive Long roomId) {
  ```
- `GlobalExceptionHandler`의 `ConstraintViolationException` 핸들러는 이미 있음 (직전 작업에서 정리)
- 검증: SC2 재현 시 음수 → 즉시 400 + 명확한 메시지

### PR3: SecurityConfig permitAll 정리 ⭐
- `/api/v1/openvidu/**` → 인증 필요로 변경
- `/api/v1/rooms/**` → 인증 필요로 변경 (단 GET 목록은 공개로 둘 수도, 정책 결정)
- `/api/v1/contents/**` → 인증 필요
- `/api/v1/members/login`, `/api/v1/members/signup` 라인 삭제 (dead route — PR8에서)
- 주석 처리된 line 80~86 블록 삭제
- `/webrtc-test.html` permitAll 삭제 (PR9에서)
- 환경별 분기 도입: `@Profile("dev")`로 dev에서만 추가 permitAll 허용
- 검증: SC3 매트릭스 재실행 시 모든 endpoint 401

### PR4: JwtUtil 일괄 초기화
- `JwtUtil.java`:
  ```java
  @Value("${jwt.secret-key}")
  private String secretKey;
  
  private SecretKey signingKey;
  
  @PostConstruct
  public void init() {
      this.signingKey = new SecretKeySpec(
          secretKey.getBytes(StandardCharsets.UTF_8),
          Jwts.SIG.HS256.key().build().getAlgorithm()
      );
  }
  ```
- `getSecretKey()` 메서드 제거, 직접 `signingKey` 참조
- 검증: SC4 재현 시 init 1회만, race 없음

### PR5: 만료 AT logout 처리
- `TokenBlacklistService.addToBlacklist` 안에서 `ExpiredJwtException` catch:
  ```java
  public void addToBlacklist(String token) {
      try {
          long remainingTime = jwtUtil.getRemainingTime(token);
          if (remainingTime > 0) {
              redisTemplate.opsForValue().set(...);
          }
      } catch (ExpiredJwtException e) {
          log.debug("이미 만료된 토큰, 블랙리스트 추가 생략");
      }
  }
  ```
- 또는 SC5 결과에 따라 logout API에서 사전 차단 (필터에서 막히면 불필요)
- 검증: SC5 재현 시 500 안 남

### PR6: 다중 디바이스 정책 결정 + 구현
- 의사결정: 단일 디바이스 강제 vs 다중 디바이스 허용
- 단일 강제(현재): 현재 동작 그대로, 단 응답 메시지에 "다른 기기에서 로그인되었습니다" 명시
- 다중 허용: 키를 `refresh:{email}:{deviceId}`로 변경. deviceId는 클라이언트에서 generate 또는 헤더로 전달
- 검증: SC6 재현 시 정책에 맞는 동작

### PR7: AuthService.refreshAccessToken catch 세분화
- `AuthService.java:56~61`:
  ```java
  catch (BusinessException e) {
      throw e;
  } catch (ExpiredJwtException e) {
      throw new BusinessException(ErrorCode.EXPIRED_TOKEN_ERROR);
  } catch (JwtException e) {
      throw new BusinessException(ErrorCode.INVALID_TOKEN_ERROR);
  } catch (Exception e) {
      log.error("Refresh token 검증 실패", e);
      throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
  }
  ```
- 검증: SC7 재현 시 만료 RT는 EXPIRED_TOKEN_ERROR로 응답

### PR8: dead route 정리
- SC8 결과에 따라:
  - 라우팅 안 됨 확인 시: `SecurityConfig.java:111`의 `/api/v1/members/login`, `/api/v1/members/signup` 라인 삭제
  - 만약 사용 중이면 명확화

### PR9: 테스트 페이지 운영 차단
- `WebSecurityCustomizer` 또는 `authorizeHttpRequests`에서 `/webrtc-test.html`, `/websocket-test.html` 제거
- `@Profile("dev")`에서만 허용 또는 빌드 시점에 dist에서 제외
- 검증: SC9 재현 시 운영 모드에서 404 또는 401

---

## 측정 지표 표 (Before/After 채우기)

| 지표 | Before | After (목표) |
|---|---|---|
| 약한 비밀번호 통과율 (SC1) | 100% | 0% |
| 음수 PathVariable 응답 시간 (SC2) | 깊은 레이어에서 처리 ms | 즉시 400 |
| permitAll 인증 우회 가능 endpoint 수 (SC3) | (측정) | 0 (의도된 공개만) |
| JwtUtil init race 발생 (SC4) | (측정) | 0 |
| 만료 AT logout 시 500 (SC5) | (측정) | 0 |
| 다중 디바이스 처리 의도 명시 (SC6) | 모호 | 정책 명시 |
| RT 만료 사유 응답 정확도 (SC7) | 0% (모두 INVALID) | 100% |
| dead route 응답 (SC8) | (404/405) | 정리됨 |
| 테스트 페이지 운영 노출 (SC9) | 노출 | 차단 |

---

## 정리 산출물

- `meari-be/md/_auth_session_ts_raw.md` — 시나리오별 raw 노트 (특히 SC3 매트릭스가 핵심)
- `meari-be/md/portfolio_features.md` 신규 또는 #7 보강:
  - "운영 진입 직전 보안 매트릭스 작성 — N개 엔드포인트 인증 우회 가능 발견 후 정리"
  - "JwtUtil lazy init thread-safety — 부하 시뮬로 race 검증 후 @PostConstruct 일괄 초기화"
  - "Refresh Token catch 세분화 — 사용자에게 만료/위변조 사유 정확히 노출"

---

## 진행 체크리스트

- [ ] Pre-flight (Postman, BE 로컬, AT 만료 시간 단축)
- [ ] SC1 약한 비밀번호
- [ ] SC2 PathVariable 음수
- [ ] SC3 ⭐ permitAll 매트릭스
- [ ] SC4 JwtUtil race
- [ ] SC5 만료 AT logout
- [ ] SC6 다중 디바이스
- [ ] SC7 refresh broad catch
- [ ] SC8 dead route
- [ ] SC9 테스트 페이지 노출
- [ ] PR1 비밀번호 @Pattern
- [ ] PR2 PathVariable @Positive
- [ ] PR3 SecurityConfig permitAll 정리
- [ ] PR4 JwtUtil @PostConstruct
- [ ] PR5 만료 AT 안전 처리
- [ ] PR6 다중 디바이스 정책
- [ ] PR7 refresh catch 세분화
- [ ] PR8 dead route 정리
- [ ] PR9 테스트 페이지 차단
- [ ] 측정 지표 표 채우기
- [ ] portfolio 갱신

---

**작성일**: 2026-04-29
**다음 액션**: AT 만료 시간 단축 → SC1부터 순차 진행 (가장 빠른 수확은 SC3 매트릭스)