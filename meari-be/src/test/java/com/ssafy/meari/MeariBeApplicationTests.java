package com.ssafy.meari;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("풀 컨텍스트 부팅 — 외부 인프라(RabbitMQ/FastAPI) 의존, 환경 분리 후 재활성화")
class MeariBeApplicationTests {

    @Test
    void contextLoads() {
    }

}
