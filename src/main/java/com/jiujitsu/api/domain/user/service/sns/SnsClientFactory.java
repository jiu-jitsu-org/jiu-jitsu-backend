package com.jiujitsu.api.domain.user.service.sns;

import com.jiujitsu.api.domain.user.entity.SnsProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SnsClientFactory {

    private final KakaoSnsClient kakaoSnsClient;
    private final GoogleSnsClient googleSnsClient;
    private final AppleSnsClient appleSnsClient;

    public SnsClient getClient(SnsProvider snsProvider) {
        return switch (snsProvider) {
            case KAKAO -> kakaoSnsClient;
            case GOOGLE -> googleSnsClient;
            case APPLE -> appleSnsClient;
            case NONE -> throw new IllegalArgumentException("NONE provider does not support SNS login");
        };
    }
}
