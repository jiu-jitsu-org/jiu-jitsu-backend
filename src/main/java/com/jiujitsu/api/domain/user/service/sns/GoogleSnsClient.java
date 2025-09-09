package com.jiujitsu.api.domain.user.service.sns;

import com.fasterxml.jackson.databind.JsonNode;
import com.jiujitsu.api.domain.user.dto.SnsUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class GoogleSnsClient implements SnsClient {

    private final WebClient webClient;
    private static final String GOOGLE_USER_INFO_URL = "https://www.googleapis.com/oauth2/v2/userinfo";

    @Override
    public SnsUserInfo getUserInfo(String accessToken, String idToken) {
        try {
            JsonNode response = webClient.get()
                    .uri(GOOGLE_USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("구글 사용자 정보를 가져올 수 없습니다");
            }

            String snsId = response.get("id").asText();
            String email = response.has("email") ? response.get("email").asText() : null;
            String nickname = response.has("name") ? response.get("name").asText() : "구글 사용자";
            String profileImageUrl = response.has("picture") ? response.get("picture").asText() : null;

            return new SnsUserInfo(snsId, email, nickname, profileImageUrl);

        } catch (Exception e) {
            log.error("구글 사용자 정보 조회 실패", e);
            throw new RuntimeException("구글 로그인 처리 중 오류가 발생했습니다", e);
        }
    }
}
