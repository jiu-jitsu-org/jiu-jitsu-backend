package com.jiujitsu.api.domain.admin.service;

import com.jiujitsu.api.domain.admin.dto.AdminLoginRequest;
import com.jiujitsu.api.domain.admin.dto.AdminLoginResponse;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.repository.UserRepository;
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

    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    public AdminLoginResponse login(AdminLoginRequest request) {
        User admin = userRepository.findByEmailAndRole(request.email(), UserRole.ADMIN)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            throw new ErrorException(ErrorCode.AUTHENTICATION_FAILED);
        }

        String accessToken = jwtTokenProvider.createAccessToken(admin.getId(), admin.getEmail(), UserRole.ADMIN);
        String refreshToken = jwtTokenProvider.createRefreshToken(admin.getId());

        return new AdminLoginResponse(accessToken, refreshToken);
    }
}
