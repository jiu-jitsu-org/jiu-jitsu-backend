package com.jiujitsu.api;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.task.SyncTaskExecutor;

import java.util.concurrent.Executor;

/**
 * 테스트에서는 pushExecutor 를 동기 실행기로 대체해 @Async 메서드가 같은 스레드에서 완료된 뒤
 * 검증할 수 있게 한다.
 */
@TestConfiguration
public class TestAsyncConfig {

    @Bean(name = "pushExecutor")
    public Executor pushExecutor() {
        return new SyncTaskExecutor();
    }
}
