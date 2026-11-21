package com.jiujitsu.api.domain.admin.service;

import com.jiujitsu.api.domain.admin.dto.AdminLoginRequest;
import com.jiujitsu.api.domain.admin.dto.AdminLoginResponse;
import com.jiujitsu.api.domain.admin.dto.AdminLogoutRequest;
import com.jiujitsu.api.domain.admin.dto.AdminRefreshRequest;
import com.jiujitsu.api.domain.admin.dto.AdminSignupRequest;
import com.jiujitsu.api.domain.admin.entity.Account;
import com.jiujitsu.api.domain.admin.entity.AccountRole;
import com.jiujitsu.api.domain.admin.repository.AccountRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.security.JwtTokenProvider;
import com.jiujitsu.api.global.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminAuthService {

    private final AccountRepository accountRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public AdminLoginResponse signup(AdminSignupRequest request) {
        if (accountRepository.existsByEmail(request.email())) {
            throw new ErrorException(ErrorCode.EMAIL_DUPLICATED);
        }

        Account account = Account.builder()
                .email(request.email())
                .passwordHash(passwordEncoder.encode(request.password()))
                .name(request.name())
                .role(AccountRole.OPERATOR)
                .build();

        accountRepository.save(account);

        return generateTokens(account);
    }

    public AdminLoginResponse login(AdminLoginRequest request) {
        Account account = accountRepository.findByEmail(request.email())
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), account.getPasswordHash())) {
            throw new ErrorException(ErrorCode.AUTHENTICATION_FAILED);
        }

        return generateTokens(account);
    }

    public AdminLoginResponse refresh(AdminRefreshRequest request) {
        String refreshToken = request.refreshToken();
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ErrorException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        if (!"refresh".equals(jwtTokenProvider.getTokenTypeFromToken(refreshToken))) {
            throw new ErrorException(ErrorCode.NOT_MATCH_CATEGORY);
        }

        Long accountId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        tokenBlacklistService.blacklistToken(refreshToken);
        return generateTokens(account);
    }

    public void logout(AdminLogoutRequest request) {
        blacklistIfValid(request.accessToken());
        blacklistIfValid(request.refreshToken());
    }

    private void blacklistIfValid(String token) {
        try {
            if (jwtTokenProvider.validateToken(token)) {
                tokenBlacklistService.blacklistToken(token);
            }
        } catch (ErrorException ignored) {
            // 만료된 토큰도 블랙리스트에 추가
            tokenBlacklistService.blacklistToken(token);
        }
    }

    private AdminLoginResponse generateTokens(Account account) {
        String accessToken = jwtTokenProvider.createAccessToken(account.getId(), account.getEmail(), account.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(account.getId());
        return new AdminLoginResponse(accessToken, refreshToken);
    }
}
