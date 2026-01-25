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