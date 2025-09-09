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
public class KakaoSnsClient implements SnsClient {

    private final WebClient webClient;
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Override
    public SnsUserInfo getUserInfo(String accessToken, String idToken) {
        try {
            JsonNode response = webClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                throw new RuntimeException("카카오 사용자 정보를 가져올 수 없습니다");
            }

            String snsId = response.get("id").asText();
            JsonNode kakaoAccount = response.get("kakao_account");
            JsonNode profile = kakaoAccount.get("profile");

            String email = kakaoAccount.has("email") ? kakaoAccount.get("email").asText() : null;
            String nickname = profile.has("nickname") ? profile.get("nickname").asText() : "카카오 사용자";
            String profileImageUrl = profile.has("profile_image_url") ? 
                    profile.get("profile_image_url").asText() : null;

            return new SnsUserInfo(snsId, email, nickname, profileImageUrl);

        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new RuntimeException("카카오 로그인 처리 중 오류가 발생했습니다", e);
        }
    }
}
