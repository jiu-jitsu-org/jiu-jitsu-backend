package com.jiujitsu.api.global.security;


import com.auth0.jwt.interfaces.Claim;
import com.jiujitsu.api.domain.user.dto.TempUserInfo;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Slf4j
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityInMilliseconds;
    private final long refreshTokenValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret:jiujitsu-secret-key-for-development-only-change-in-production}") String secret,
            @Value("${jwt.access-token-validity:3600000}") long accessTokenValidityInMilliseconds,
            @Value("${jwt.refresh-token-validity:604800000}") long refreshTokenValidityInMilliseconds) {
        
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInMilliseconds = accessTokenValidityInMilliseconds;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInMilliseconds;
    }

    public String createAccessToken(Long userId, String email) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + accessTokenValidityInMilliseconds);

        return Jwts.builder().subject(userId.toString())
                .claim("email", email)
                .claim("type", "access").issuedAt(now).expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        Date validity = new Date(now.getTime() + refreshTokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public String createTemporaryToken(String userId, String email, SnsProvider snsProvider) {
        Date now = new Date();
        long temporaryTokenValidityInMilliseconds = 5 * 60 * 1000; // 5분
        Date validity = new Date(now.getTime() + temporaryTokenValidityInMilliseconds);

        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("type", "temporary")
                .claim("snsProvider", snsProvider)
                .issuedAt(now)
                .expiration(validity)
                .signWith(secretKey)
                .compact();
    }

    public Claims getJWTClaims(String token) {
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public Long getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return Long.valueOf(claims.getSubject());
    }

    public String getEmailFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("email", String.class);
    }

    public String getTokenTypeFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        
        return claims.get("type", String.class);
    }

    //todo: access, refresh 파싱도 추가 작업 진행 필요
    public TempUserInfo parseTemporaryToken(String token) {
        Claims claims = parseAndValidate(token);

        validateTokenType(claims, "temporary");

        String snsId = claims.getSubject();
        String email = claims.get("email", String.class);
        String provider = claims.get("snsProvider", String.class);

        return new TempUserInfo(
                snsId,
                email,
                SnsProvider.valueOf(provider)
        );
    }

    private Claims parseAndValidate(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            throw new ErrorException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateTokenType(Claims claims, String expectedType) {
        String type = claims.get("type", String.class);

        if (!expectedType.equals(type)) {
            throw new ErrorException(ErrorCode.NOT_MATCH_CATEGORY);
        }
    }
}
