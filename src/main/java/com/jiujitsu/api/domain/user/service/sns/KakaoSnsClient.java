package com.jiujitsu.api.domain.user.service.sns;

import com.fasterxml.jackson.databind.JsonNode;
import com.jiujitsu.api.domain.user.dto.SnsUserInfo;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
@RequiredArgsConstructor
public class KakaoSnsClient implements SnsClient {

    private final WebClient webClient;
    private static final String KAKAO_USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";
    private static final String KAKAO_UNLINK_URL = "https://kapi.kakao.com/v1/user/unlink";

    @Value("${kakao.adminKey}")
    private String adminKey;

    @Override
    public SnsUserInfo getUserInfo(String accessToken) {
        if (StringUtils.equals(accessToken, "kakaoTest")) {
            return new SnsUserInfo("testid", "test@jujitsu.com");
        }
        if (StringUtils.equals(accessToken, "kakaoTest2")) {
            return new SnsUserInfo("testid2", "test2@jujitsu.com");
        }
        try {
            JsonNode response = webClient.get()
                    .uri(KAKAO_USER_INFO_URL)
                    .header("Authorization", "Bearer " + accessToken)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) {
                throw new ErrorException(ErrorCode.INVALID_TOKEN);
            }

            String snsId = response.get("id").asText();
            JsonNode kakaoAccount = response.get("kakao_account");
            String email = kakaoAccount.has("email") ? kakaoAccount.get("email").asText() : null;

            return new SnsUserInfo(snsId, email);

        } catch (Exception e) {
            log.error("카카오 사용자 정보 조회 실패", e);
            throw new ErrorException(ErrorCode.KEY_PARSING_ERROR);
        }
    }

    @Override
    public void unlink(String snsId, String authorizationCode) {
        try {
            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("target_id_type", "user_id");
            body.add("target_id", snsId);

            webClient.post()
                    .uri(KAKAO_UNLINK_URL)
                    .header("Authorization", "KakaoAK " + adminKey)
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            log.info("카카오 연결 해제 완료: snsId={}", snsId);
        } catch (Exception e) {
            log.error("카카오 연결 해제 실패: snsId={}", snsId, e);
        }
    }
}
