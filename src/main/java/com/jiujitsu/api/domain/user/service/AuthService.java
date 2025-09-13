package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.dto.AuthResponse;
import com.jiujitsu.api.domain.user.dto.LogoutRequest;
import com.jiujitsu.api.domain.user.dto.LogoutResponse;
import com.jiujitsu.api.domain.user.dto.SnsLoginRequest;
import com.jiujitsu.api.domain.user.dto.SnsUserInfo;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.sns.SnsClient;
import com.jiujitsu.api.domain.user.service.sns.SnsClientFactory;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.security.JwtTokenProvider;
import com.jiujitsu.api.global.security.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final SnsClientFactory snsClientFactory;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;

    public AuthResponse snsLogin(SnsLoginRequest request) {
        // SNS 클라이언트를 통해 사용자 정보 조회
        SnsClient snsClient = snsClientFactory.getClient(request.getSnsProvider());
        SnsUserInfo snsUserInfo = snsClient.getUserInfo(request.getAccessToken(), request.getIdToken());

        // 기존 사용자 조회 또는 새 사용자 생성
        User user = userRepository.findBySnsProviderAndSnsId(request.getSnsProvider(), snsUserInfo.getSnsId())
                .orElseGet(() -> createNewUser(request.getSnsProvider(), snsUserInfo));

        // 사용자 정보 업데이트 (프로필 정보가 변경되었을 수 있음)
        updateUserInfo(user, snsUserInfo);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 응답 생성
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getSnsProvider().name()
        );

        return new AuthResponse(accessToken, refreshToken, userInfo);
    }

    private User createNewUser(SnsProvider snsProvider, SnsUserInfo snsUserInfo) {
        // 이메일 중복 체크
        if (snsUserInfo.getEmail() != null && userRepository.existsByEmail(snsUserInfo.getEmail())) {
            throw new RuntimeException("이미 등록된 이메일입니다: " + snsUserInfo.getEmail());
        }

        User newUser = User.builder()
                .email(snsUserInfo.getEmail())
                .nickname(snsUserInfo.getNickname())
                .profileImageUrl(snsUserInfo.getProfileImageUrl())
                .snsProvider(snsProvider)
                .snsId(snsUserInfo.getSnsId())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(newUser);
    }

    private void updateUserInfo(User user, SnsUserInfo snsUserInfo) {
        // 프로필 정보가 변경되었을 경우 업데이트
        boolean needsUpdate = false;

        if (snsUserInfo.getNickname() != null && !snsUserInfo.getNickname().equals(user.getNickname())) {
            needsUpdate = true;
        }

        if (snsUserInfo.getProfileImageUrl() != null && !snsUserInfo.getProfileImageUrl().equals(user.getProfileImageUrl())) {
            needsUpdate = true;
        }

        if (needsUpdate) {
            user.updateProfile(snsUserInfo.getNickname(), snsUserInfo.getProfileImageUrl());
            userRepository.save(user);
        }
    }

    public AuthResponse refreshToken(String refreshToken) {
        // 리프레시 토큰 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ErrorException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 토큰 타입 확인
        String tokenType = jwtTokenProvider.getTokenTypeFromToken(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new ErrorException(ErrorCode.NOT_MATCH_CATEGORY);
        }

        // 사용자 정보 조회
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        // 새로운 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 응답 생성
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getSnsProvider().name()
        );

        return new AuthResponse(newAccessToken, newRefreshToken, userInfo);
    }

    public LogoutResponse logout(LogoutRequest request) {
        // 액세스 토큰 검증 및 블랙리스트 추가
        if (request.getAccessToken() != null && !request.getAccessToken().trim().isEmpty()) {
            if (jwtTokenProvider.validateToken(request.getAccessToken())) {
                tokenBlacklistService.blacklistToken(request.getAccessToken());
                log.info("Access token blacklisted for logout");
            } else {
                log.warn("Invalid access token provided for logout");
            }
        }

        // 리프레시 토큰이 제공된 경우 블랙리스트 추가
        if (request.getRefreshToken() != null && !request.getRefreshToken().trim().isEmpty()) {
            if (jwtTokenProvider.validateToken(request.getRefreshToken())) {
                tokenBlacklistService.blacklistToken(request.getRefreshToken());
                log.info("Refresh token blacklisted for logout");
            } else {
                log.warn("Invalid refresh token provided for logout");
            }
        }

        return new LogoutResponse(true, "로그아웃이 완료되었습니다");
    }
}
