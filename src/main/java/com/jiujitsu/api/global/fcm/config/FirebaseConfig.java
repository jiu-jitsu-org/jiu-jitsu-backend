package com.jiujitsu.api.global.fcm.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.io.IOException;

@Configuration
@Profile("!test") // 테스트는 TestFcmConfig의 목 FirebaseMessaging을 쓴다
public class FirebaseConfig {
    private FirebaseApp firebaseApp;

    @PostConstruct
    public void initializeFcm() {
        try {
            if (!FirebaseApp.getApps().isEmpty()) {
                firebaseApp = FirebaseApp.getInstance();
                return;
            }

            FirebaseOptions options = FirebaseOptions.builder()
                    .setCredentials(GoogleCredentials.getApplicationDefault())
                    .build();

            firebaseApp = FirebaseApp.initializeApp(options);

        } catch (Exception e) {
            throw new IllegalStateException("Firebase 초기화 실패", e);
        }
    }

    @Bean
    public FirebaseMessaging initFirebaseMessaging() {
        return FirebaseMessaging.getInstance(firebaseApp);
    }
}
