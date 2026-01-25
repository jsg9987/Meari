# 1. 프로젝트 아키텍쳐 및 원칙(구조)

- Layered Architecture 준수: "Controller - Service - Repository - Entity 계층을 엄격히 분리하고, 각 계층 간의 의존성 방향을 한 방향으로만 유지해."
- Entity와 DTO 분리: requestDto, responseDto를 생성해서 변환해야해. Service는 DTO를 반환하고, Controller는 이를 그대로 응답 
- 비즈니스 로직 위치: "Service 계층은 트랜잭션과 흐름 제어만 담당하고, 도메인 로직은 가급적 Entity 내부(도메인 모델 패턴)에 구현해."
- 의존성 주입 방식: @Autowired 대신 @RequiredArgsConstructor를 통한 생성자 주입을 사용 

# 2. TDD 및 테스트 코드 지시
- TDD 우선 원칙: "모든 기능 개발 전에 요구사항을 검증할 수 있는 테스트 코드를 먼저 작성해 줘."
- 테스트 범위 지정: "단위 테스트(Unit Test)는 Mockito를 사용해 외부 의존성을 제거하고, 통합 테스트(Integration Test)는 @SpringBootTest를 활용해서 만들어. 다만 테스트를 실제로 실행시키지는마. 너가 만들면 내가 실행해서 테스트해볼게."
- 경계값 테스트: "성공 케이스뿐만 아니라 반드시 **실패 케이스(예외 상황, 유효하지 않은 입력 값)**에 대한 테스트도 포함해."
- Given-When-Then 패턴

# 3. 예외 처리 및 공통 응답 규격
   이 부분을 놓치면 나중에 프론트엔드와 통신할 때 에러 처리가 제각각이 됩니다.
- Global Exception Handler: "중앙 집중식 예외 처리를 위해 @RestControllerAdvice를 사용하고, 커스텀 Exception 클래스를 정의해서 사용해."
- 공통 응답 포맷: 우리 프로젝트에서 정한 ApiResonse(공통 응답 구조가 있어.) 이걸 지켜서 응답해줘야해
- 우리 프로젝트에서 공통 Error처리 방식이 있어. GlobalException과 BusinessException을 활용해서 에러처리 해줘.
- 

# 4. 데이터베이스 및 기술 세부 지시
- 우리 DB와 관련없는 member 예시 entity야. 스타일만 숙지하고 적용은 우리 db에 맞게 해줘.
```
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Entity
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String email;
    
    private String password;
    
    private String nickname;

    public void updateNickname(String nickname) {
        this.nickname = nickname;
    }

    @Builder
    public Member(String email, String password) {
        this.email = email;
        this.password = password;
        this.nickname = "닉네임을 설정하세요";
    }

    @Builder
    public Member(String email, String password, String nickname) {
        this.email = email;
        this.password = password;
        this.nickname = nickname;
    }
}
```
- JPA 생성자 규칙이야. 예시일 뿐 이부분에 대해서 필요한 점이 있다면 적용하고 보고해줘.  
- AllArgs 남발을 주의하기 위해 @Bilder를 생성자마다 적용하도록했어. 
- Logging: "주요 로직의 시작과 끝, 특히 예외 발생 지점에는 Slf4j를 사용해 의미 있는 로그를 남겨줘."
- Dirty Checking 활용해 도메인 메서드 설계해.
- Swagger(SpringDoc) 적용: "모든 컨트롤러와 DTO에는 API 문서화를 위해 Swagger 어노테이션(@Tag, @Operation, @Schema)을 상세히 작성해."