package com.ssafy.meari;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class MeariBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(MeariBeApplication.class, args);
    }

}
