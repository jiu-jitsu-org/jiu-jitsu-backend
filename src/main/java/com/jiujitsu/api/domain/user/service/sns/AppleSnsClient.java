package com.jiujitsu.api.domain.user.service.sns;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiujitsu.api.domain.user.dto.SnsUserInfo;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleSnsClient implements SnsClient {

    private final ObjectMapper objectMapper;

    @Override
    public SnsUserInfo getUserInfo(String accessToken, String idToken) {
        try {
            if (idToken == null || idToken.trim().isEmpty()) {
                throw new RuntimeException("Apple 로그인에는 ID 토큰이 필요합니다");
            }

            // JWT ID 토큰에서 사용자 정보 추출
            String[] tokenParts = idToken.split("\\.");
            if (tokenParts.length != 3) {
                throw new RuntimeException("유효하지 않은 Apple ID 토큰입니다");
            }

            // JWT payload 디코딩
            String payload = new String(Base64.getUrlDecoder().decode(tokenParts[1]));
            JsonNode claims = objectMapper.readTree(payload);

            String snsId = claims.get("sub").asText();
            String email = claims.has("email") ? claims.get("email").asText() : null;
            
            // Apple은 이름과 프로필 이미지를 ID 토큰에 포함하지 않음
            // 앱에서 처음 로그인 시 별도로 전달받아야 함
            String nickname = "Apple 사용자";
            String profileImageUrl = null;

            return new SnsUserInfo(snsId, email, nickname, profileImageUrl);

        } catch (Exception e) {
            log.error("Apple 사용자 정보 조회 실패", e);
            throw new RuntimeException("Apple 로그인 처리 중 오류가 발생했습니다", e);
        }
    }
}
