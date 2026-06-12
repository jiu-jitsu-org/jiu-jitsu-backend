package com.jiujitsu.api.global.security;

import com.jiujitsu.api.domain.user.dto.TempUserInfo;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.UserRole;
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
    private final long temporaryTokenValidityInMilliseconds;

    public JwtTokenProvider(
            @Value("${jwt.secret:jiujitsu-secret-key-for-development-only-change-in-production}") String secret,
            @Value("${jwt.access-token-validity:3600000}") long accessTokenValidityInMilliseconds,
            @Value("${jwt.refresh-token-validity:604800000}") long refreshTokenValidityInMilliseconds,
            @Value("${jwt.temporary-token-validity:300000}") long temporaryTokenValidityInMilliseconds) {

        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityInMilliseconds = accessTokenValidityInMilliseconds;
        this.refreshTokenValidityInMilliseconds = refreshTokenValidityInMilliseconds;
        this.temporaryTokenValidityInMilliseconds = temporaryTokenValidityInMilliseconds;
    }

    public String createAccessToken(Long userId, String email, UserRole role) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("email", email)
                .claim("role", role != null ? role.name() : null)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + accessTokenValidityInMilliseconds))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshTokenValidityInMilliseconds))
                .signWith(secretKey)
                .compact();
    }

    public String createTemporaryToken(String userId, String email, SnsProvider snsProvider) {
        Date now = new Date();
        return Jwts.builder()
                .subject(userId)
                .claim("email", email)
                .claim("type", "temporary")
                .claim("snsProvider", snsProvider)
                .issuedAt(now)
                .expiration(new Date(now.getTime() + temporaryTokenValidityInMilliseconds))
                .signWith(secretKey)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }

    public Claims getJWTClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();
    }

    public Long getUserIdFromToken(String token) {
        return Long.valueOf(getJWTClaims(token).getSubject());
    }

    public String getEmailFromToken(String token) {
        return getJWTClaims(token).get("email", String.class);
    }

    public String getTokenTypeFromToken(String token) {
        return getJWTClaims(token).get("type", String.class);
    }

    public String getRoleFromToken(String token) {
        return getJWTClaims(token).get("role", String.class);
    }

    //todo: access, refresh 파싱도 추가 작업 진행 필요
    public TempUserInfo parseTemporaryToken(String token) {
        Claims claims = parseAndValidate(token);
        validateTokenType(claims, "temporary");
        return new TempUserInfo(
                claims.getSubject(),
                claims.get("email", String.class),
                SnsProvider.valueOf(claims.get("snsProvider", String.class))
        );
    }

    private Claims parseAndValidate(String token) {
        try {
            return getJWTClaims(token);
        } catch (JwtException | IllegalArgumentException e) {
            log.debug("Invalid JWT token: {}", e.getMessage());
            throw new ErrorException(ErrorCode.INVALID_TOKEN);
        }
    }

    private void validateTokenType(Claims claims, String expectedType) {
        if (!expectedType.equals(claims.get("type", String.class))) {
            throw new ErrorException(ErrorCode.NOT_MATCH_CATEGORY);
        }
    }
}
