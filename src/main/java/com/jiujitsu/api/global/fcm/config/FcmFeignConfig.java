package com.jiujitsu.api.global.fcm.config;

import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.fcm.provider.FcmTokenProvider;
import feign.RequestInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@RequiredArgsConstructor
public class FcmFeignConfig {

    private final FcmTokenProvider fcmTokenProvider;

    @Bean
    public RequestInterceptor fcmInterceptor() {
        return requestTemplate -> {
            try {
                String accessToken = fcmTokenProvider.getAccessToken();

                requestTemplate.header(
                        "Authorization",
                        "Bearer " + accessToken
                );
                requestTemplate.header("Content-Type", "application/json");
            } catch (IOException e) {
                throw new ErrorException(ErrorCode.NO_TOKEN);
            }
        };
    }
}
