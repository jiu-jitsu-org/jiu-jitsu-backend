package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.dto.*;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.factory.UserFactory;
import com.jiujitsu.api.domain.user.mapper.AuthMapper;
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

import static org.apache.commons.lang3.StringUtils.isBlank;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final SnsClientFactory snsClientFactory;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthMapper authMapper;
    private final UserFactory userFactory;

    // 테스트 로그인
    public AuthResponse testLogin(SnsLoginRequest request) {
        User user = userRepository.findBySnsProviderAndNickname(request.snsProvider(), request.accessToken())
                .orElseThrow();

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 응답 세팅
        return authMapper.toAccessResponse(
                accessToken,
                refreshToken,
                userFactory.createUserInfo(user, false)
        );
    }
    /**
     * SNS Login - 로그인 분리
     */
    public AuthResponse snsLogin(SnsLoginRequest request) {
        // SNS 사용자 정보 조회
        SnsUserInfo snsUserInfo = getSnsUserInfo(request);

        // response 세팅
        return userRepository
                .findBySnsProviderAndSnsId(request.snsProvider(), snsUserInfo.getSnsId())
                .map(user -> handleExistingUser(user, snsUserInfo)) // 기존회원 처리
                .orElseGet(() -> handleNewUser(request, snsUserInfo));  // 신규회원 처리
    }

    /**
     * SNS 로그인 - snsUserInfo 조회
     */
    private SnsUserInfo getSnsUserInfo(SnsLoginRequest request) {
        SnsClient snsClient = snsClientFactory.getClient(request.snsProvider());
        return snsClient.getUserInfo(request.accessToken());
    }

    /**
     * SNS 로그인 - 기존 유저 처리
     */
    private AuthResponse handleExistingUser(User user, SnsUserInfo snsUserInfo) {
        // 탈퇴회원 확인
        boolean deactivatedWithinGrace = handleUserStatus(user);

        // 회원정보 업데이트
        updateUserInfo(user, snsUserInfo);

        // 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 응답 세팅
        return authMapper.toAccessResponse(
                accessToken,
                refreshToken,
                userFactory.createUserInfo(user, deactivatedWithinGrace)
        );
    }

    /**
     * SNS 로그인 - 탈퇴/복구
     */
    private boolean handleUserStatus(User user) {
        if (user.getStatus() != UserStatus.DELETED) {
            return false;
        }

        if (!user.isWithinGracePeriod()) {
            throw new ErrorException(ErrorCode.USER_ACCOUNT_EXPIRED);
        }

        user.updateStatus(UserStatus.ACTIVE);
        return true;
    }

    /**
     * SNS 로그인 - 신규 유저 처리
     */
    private AuthResponse handleNewUser(SnsLoginRequest request, SnsUserInfo snsUserInfo) {
        String tempToken = jwtTokenProvider.createTemporaryToken(
                snsUserInfo.getSnsId(),
                snsUserInfo.getEmail(),
                request.snsProvider()
        );

        return authMapper.toTempResponse(tempToken);
    }

    /**
     * 프로필 정보 업데이트
     */
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
        }
    }

    /**
     * 토큰 갱신
     */
    public AuthResponse refreshToken(String refreshToken) {
        Long userId = validateAndExtractUserId(refreshToken);

        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        // 새로운 토큰 생성
        String newAccessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String newRefreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        // 응답 생성
        UserInfo userInfo = userFactory.createUserInfo(user, false);

        return authMapper.toAccessResponse(newAccessToken, newRefreshToken, userInfo);
    }

    public Long validateAndExtractUserId(String refreshToken) {
        // 리프레시 토큰 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new ErrorException(ErrorCode.INVALID_REFRESH_TOKEN);
        }

        // 토큰 타입 확인
        String tokenType = jwtTokenProvider.getTokenTypeFromToken(refreshToken);
        if (!"refresh".equals(tokenType)) {
            throw new ErrorException(ErrorCode.NOT_MATCH_CATEGORY);
        }

        return jwtTokenProvider.getUserIdFromToken(refreshToken);
    }

    public LogoutResponse logout(LogoutRequest request) {
        processLogoutToken(request.accessToken());
        processLogoutToken(request.refreshToken());

        return authMapper.toLogoutResponse();
    }

    private void processLogoutToken(String token) {
        if (isBlank(token)) {
            return;
        }

        if (!jwtTokenProvider.validateToken(token)) {
            return;
        }
        tokenBlacklistService.blacklistToken(token);
    }
}
