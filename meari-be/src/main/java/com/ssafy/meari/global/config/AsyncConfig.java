package com.ssafy.meari.global.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Slf4j
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "geminiAnalysisExecutor")
    public Executor geminiAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(5);
        executor.setQueueCapacity(10);
        executor.setThreadNamePrefix("gemini-analysis-");
        executor.initialize();
        return executor;
    }

    /**
     * 발음 분석 전용 스레드 풀
     * - 코어 스레드: 4개
     * - 최대 스레드: 8개
     * - 큐 용량: 100개
     */
    @Bean(name = "analysisTaskExecutor")
    public Executor analysisTaskExecutor() {
        log.info("비동기 작업 실행기 생성: analysisTaskExecutor");

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("analysis-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(60);
        executor.initialize();

        log.info("analysisTaskExecutor 설정 완료: corePoolSize=4, maxPoolSize=8, queueCapacity=100");
        return executor;
    }
}
