package com.jiujitsu.api.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

/**
 * 오늘 날짜가 결과에 영향을 주는 로직(대회 접수 상태, 자동완성 기준일 등)이 쓰는 시계.
 * 서비스가 LocalDate.now() 대신 주입받아 쓰면 테스트에서 날짜를 고정할 수 있다.
 * JVM 기본 타임존과 같은 Asia/Seoul 로 명시한다.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}
