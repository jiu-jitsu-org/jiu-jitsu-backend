package com.jiujitsu.api.domain.user.service.sns;

import com.auth0.jwk.Jwk;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.UrlJwkProvider;
import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiujitsu.api.domain.user.dto.SnsUserInfo;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.security.interfaces.RSAPublicKey;

@Slf4j
@Component
@RequiredArgsConstructor
public class AppleSnsClient implements SnsClient {

    private final ObjectMapper objectMapper;

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
            // 1. JWT header에서 key id(kid), alg 가져오기
            DecodedJWT decodedJWT = JWT.decode(idToken);
            String kid = decodedJWT.getKeyId();
            String alg = decodedJWT.getAlgorithm();

            // 2. Apple 공개키 요청
            URL url = new URL("https://appleid.apple.com/auth/keys");
            InputStream inputStream = url.openStream();
            String jwksJson = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);

            JwkProvider provider = new UrlJwkProvider(new URL("https://appleid.apple.com/auth/keys"));
            Jwk jwk = provider.get(kid); // kid에 해당하는 공개키 가져오기

            return (RSAPublicKey) jwk.getPublicKey();
        } catch (Exception e) {
            throw new ErrorException(ErrorCode.KEY_PARSING_ERROR);
        }
    }
}
