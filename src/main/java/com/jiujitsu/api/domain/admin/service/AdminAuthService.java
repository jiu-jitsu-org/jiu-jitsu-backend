package com.jiujitsu.api.domain.admin.service;

import com.jiujitsu.api.domain.admin.dto.AdminLoginRequest;
import com.jiujitsu.api.domain.admin.dto.AdminLoginResponse;
import com.jiujitsu.api.domain.admin.dto.AdminSignupRequest;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
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

    @Transactional
    public AdminLoginResponse signup(AdminSignupRequest request) {
        // 이메일 중복 체크
        if (userRepository.existsByEmail(request.email())) {
            throw new ErrorException(ErrorCode.EMAIL_DUPLICATED);
        }

        // admin용 User 엔티티 생성(Factory는 사용하지 않음)
        User admin = User.builder()
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .snsProvider(SnsProvider.NONE)
                .snsId("ADMIN_" + request.email())
                .ownerRequested(false)
                .role(UserRole.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();

        userRepository.save(admin);

        return generateTokens(admin);
    }

    public AdminLoginResponse login(AdminLoginRequest request) {
        // 계정 체크
        User admin = userRepository.findByEmailAndRole(request.email(), UserRole.ADMIN)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        // password 체크
        if (!passwordEncoder.matches(request.password(), admin.getPassword())) {
            throw new ErrorException(ErrorCode.AUTHENTICATION_FAILED);
        }

        return generateTokens(admin);
    }

    // token 생성
    private AdminLoginResponse generateTokens(User admin) {
        String accessToken = jwtTokenProvider.createAccessToken(admin.getId(), admin.getEmail(), UserRole.ADMIN);
        String refreshToken = jwtTokenProvider.createRefreshToken(admin.getId());

        return new AdminLoginResponse(accessToken, refreshToken);
    }
}
