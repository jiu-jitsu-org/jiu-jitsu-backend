package com.jiujitsu.api.domain.admin.service;

import com.jiujitsu.api.domain.admin.dto.AdminLoginRequest;
import com.jiujitsu.api.domain.admin.dto.AdminLoginResponse;
import com.jiujitsu.api.domain.admin.dto.AdminSignupRequest;
import com.jiujitsu.api.domain.admin.entity.Account;
import com.jiujitsu.api.domain.admin.entity.AccountRole;
import com.jiujitsu.api.domain.admin.repository.AccountRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.security.JwtTokenProvider;
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

    private AdminLoginResponse generateTokens(Account account) {
        String accessToken = jwtTokenProvider.createAccessToken(account.getId(), account.getEmail(), account.getRole().name());
        String refreshToken = jwtTokenProvider.createRefreshToken(account.getId());
        return new AdminLoginResponse(accessToken, refreshToken);
    }
}
