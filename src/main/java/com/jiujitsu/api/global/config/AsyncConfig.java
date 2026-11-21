package com.jiujitsu.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 비동기 푸시 발송 설정.
 * 댓글·좋아요 등 사용자 요청 안에서 나가는 FCM 알림을 응답과 분리해 발송한다 (#229).
 */
@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * FCM 알림 발송 전용 스레드풀.
     * FCM 호출은 I/O 바운드이므로 코어 스레드를 넉넉하게 잡는다.
     * 큐가 꽉 차면 CallerRunsPolicy 로 드롭 없이 처리한다.
     */
    @Bean(name = "pushExecutor")
    public Executor pushExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(10);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("push-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
