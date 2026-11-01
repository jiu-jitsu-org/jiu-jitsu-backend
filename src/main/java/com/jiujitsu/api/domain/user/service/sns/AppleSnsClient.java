package com.jiujitsu.api.domain.user.service.sns;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiujitsu.api.domain.user.dto.SnsUserInfo;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.reactive.function.client.WebClient;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyFactory;
import java.security.interfaces.ECPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;
import java.util.Date;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleSnsClient implements SnsClient {

    private final ObjectMapper objectMapper;
    private final WebClient webClient;

    @Value("${apple.filePath}")
    private String filePath;

    @Value("${apple.keyId}")
    private String keyId;

    @Value("${apple.teamId}")
    private String teamId;

    @Value("${apple.clientId}")
    private String clientId;

    private static final String APPLE_TOKEN_URL = "https://appleid.apple.com/auth/token";
    private static final String APPLE_REVOKE_URL = "https://appleid.apple.com/auth/revoke";

    // ──────────────────────────────────────────
    // 로그인: id_token 검증
    // ──────────────────────────────────────────

    @Override
    public SnsUserInfo getUserInfo(String accessToken) {
        try {
            if (accessToken == null || accessToken.trim().isEmpty()) {
                throw new RuntimeException("Apple 로그인에는 ID 토큰이 필요합니다");
            }

            // 1. RSA 복호화
            RSAPublicKey applePublicKey = fetchApplePublicKey(accessToken);

            // 2. JWT 검증
            Algorithm algorithm = Algorithm.RSA256(applePublicKey, null);
            JWTVerifier verifier = JWT.require(algorithm).build();
            DecodedJWT decodedJWT = verifier.verify(accessToken);

            String snsId = decodedJWT.getSubject();
            String email = decodedJWT.getClaim("email").asString();

            return new SnsUserInfo(snsId, email);

        } catch (Exception e) {
            log.error("Apple 사용자 정보 조회 실패", e);
            throw new ErrorException(ErrorCode.KEY_PARSING_ERROR);
        }
    }

    private RSAPublicKey fetchApplePublicKey(String idToken) {
        try {
            DecodedJWT decodedJWT = JWT.decode(idToken);
            String kid = decodedJWT.getKeyId();

            JwkProvider provider = new UrlJwkProvider(new URL("https://appleid.apple.com/auth/keys"));
            Jwk jwk = provider.get(kid);

            return (RSAPublicKey) jwk.getPublicKey();
        } catch (Exception e) {
            throw new ErrorException(ErrorCode.KEY_PARSING_ERROR);
        }
    }

    // ──────────────────────────────────────────
    // 탈퇴: authorization_code → refresh_token → revoke
    // ──────────────────────────────────────────

    /**
     * App Store 심사 기준(5.1.1(v))에 따라 계정 삭제 시 Apple 토큰을 revoke합니다.
     *
     * @param snsId             users.snsId (Apple sub)
     * @param authorizationCode iOS Sign in with Apple 시 발급된 authorization_code
     */
    @Override
    public void unlink(String snsId, String authorizationCode) {
        if (authorizationCode == null || authorizationCode.isBlank()) {
            log.warn("Apple token revoke 건너뜀: authorizationCode 미전달 (snsId={})", snsId);
            return;
        }
        try {
            String clientSecret = generateClientSecret();
            String refreshToken = exchangeForRefreshToken(authorizationCode, clientSecret);
            revokeToken(refreshToken, clientSecret);
            log.info("Apple token revoke 완료: snsId={}", snsId);
        } catch (Exception e) {
            log.error("Apple token revoke 실패: snsId={}", snsId, e);
        }
    }

    /**
     * Apple client_secret 생성.
     * ES256으로 서명한 JWT. 유효기간 5분(교환 용도로만 사용).
     */
    private String generateClientSecret() throws Exception {
        ECPrivateKey privateKey = loadPrivateKey();
        Algorithm algorithm = Algorithm.ECDSA256(null, privateKey);
        return JWT.create()
                .withIssuer(teamId)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + 5 * 60 * 1000L))
                .withAudience("https://appleid.apple.com")
                .withSubject(clientId)
                .withKeyId(keyId)
                .sign(algorithm);
    }

    /** .p8 파일에서 EC 개인키 로드 */
    private ECPrivateKey loadPrivateKey() throws Exception {
        String raw = new String(Files.readAllBytes(Paths.get(filePath)))
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "")
                .replaceAll("\\s", "");
        byte[] keyBytes = Base64.getDecoder().decode(raw);
        PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
        return (ECPrivateKey) KeyFactory.getInstance("EC").generatePrivate(keySpec);
    }

    /** authorization_code를 Apple 서버에서 refresh_token으로 교환 */
    private String exchangeForRefreshToken(String authorizationCode, String clientSecret) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("grant_type", "authorization_code");
        body.add("code", authorizationCode);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        JsonNode response = webClient.post()
                .uri(APPLE_TOKEN_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null || !response.has("refresh_token")) {
            throw new RuntimeException("Apple token exchange 응답에 refresh_token 없음: " + response);
        }
        return response.get("refresh_token").asText();
    }

    /** refresh_token을 Apple 서버에서 revoke */
    private void revokeToken(String refreshToken, String clientSecret) {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", refreshToken);
        body.add("token_type_hint", "refresh_token");
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);

        webClient.post()
                .uri(APPLE_REVOKE_URL)
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .bodyValue(body)
                .retrieve()
                .bodyToMono(Void.class)
                .block();
    }
}
