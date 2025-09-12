package com.jiujitsu.api.global.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class TokenBlacklistService {

    private final Set<String> blacklistedTokens = ConcurrentHashMap.newKeySet();
    private final JwtTokenProvider jwtTokenProvider;

    public TokenBlacklistService(JwtTokenProvider jwtTokenProvider) {
        this.jwtTokenProvider = jwtTokenProvider;
    }

    public void blacklistToken(String token) {
        if (token != null && !token.trim().isEmpty()) {
            blacklistedTokens.add(token);
            log.info("Token blacklisted successfully");
        }
    }

    public boolean isTokenBlacklisted(String token) {
        return blacklistedTokens.contains(token);
    }

    public boolean isValidToken(String token) {
        if (token == null || token.trim().isEmpty()) {
            return false;
        }
        
        // Check if token is blacklisted
        if (isTokenBlacklisted(token)) {
            log.debug("Token is blacklisted");
            return false;
        }
        
        // Check if token is valid (not expired, proper signature)
        return jwtTokenProvider.validateToken(token);
    }

    public void clearExpiredTokens() {
        // In a production environment, you would implement a cleanup mechanism
        // to remove expired tokens from the blacklist to prevent memory leaks
        // This could be done with a scheduled task
        blacklistedTokens.removeIf(token -> {
            try {
                return !jwtTokenProvider.validateToken(token);
            } catch (Exception e) {
                // If token validation throws exception, it's likely expired or invalid
                return true;
            }
        });
        log.info("Expired tokens cleared from blacklist");
    }
}
