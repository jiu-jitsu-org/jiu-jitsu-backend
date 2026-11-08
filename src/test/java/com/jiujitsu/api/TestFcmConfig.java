package com.jiujitsu.api;

import com.google.firebase.messaging.FirebaseMessaging;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;

/**
 * 테스트 프로파일에서는 FirebaseConfig(GCP 자격증명 필요)를 끄므로,
 * FcmPushService가 주입받을 FirebaseMessaging을 목으로 대신 등록한다.
 */
@TestConfiguration
public class TestFcmConfig {

    @Bean
    FirebaseMessaging firebaseMessaging() {
        return Mockito.mock(FirebaseMessaging.class);
    }
}
