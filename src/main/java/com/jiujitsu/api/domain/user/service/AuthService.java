package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.dto.*;
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

import java.util.Optional;

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
        SnsUserInfo snsUserInfo = snsClient.getUserInfo(request.getAccessToken());

        // 회원 정보 조회
        Optional<User> userOptional = userRepository.findBySnsProviderAndSnsId(request.getSnsProvider(), snsUserInfo.getSnsId());

        if (userOptional.isPresent()) {
            // 1. 기존 회원 > 로그인
            User user = userOptional.get();

            boolean deactivatedWithinGrace = false;

            // 탈퇴 계정 처리: 30일 이내 재로그인 시 복구, 만료 시 에러
            if (user.getStatus() == UserStatus.DELETED) {
                if (user.isWithinGracePeriod()) {
                    // 복구 처리
                    user.updateStatus(UserStatus.ACTIVE); // 내부에서 deletedAt null 처리
                    userRepository.save(user);
                    deactivatedWithinGrace = true;
                } else {
                    // 이미 30일 경과하여 계정 만료
                    throw new ErrorException(ErrorCode.USER_ACCOUNT_EXPIRED);
                }
            }

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
                    user.getSnsProvider(),
                    deactivatedWithinGrace
            );

            return new AuthResponse(accessToken, refreshToken, userInfo);
        } else {
            // 2. 신규 회원 > 임시 토큰 생성
            String tempToken = jwtTokenProvider.createTemporaryToken(
                    snsUserInfo.getSnsId(), snsUserInfo.getEmail(), request.getSnsProvider());
            return new AuthResponse(tempToken);
        }
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
                user.getSnsProvider(),
                false
        );

        return new AuthResponse(newAccessToken, newRefreshToken, userInfo);
    }

    public LogoutResponse logout(LogoutRequest request) {
        // 액세스 토큰 검증 및 블랙리스트 추가
        if (request.getAccessToken() != null && !request.getAccessToken().trim().isEmpty()) {
            if (jwtTokenProvider.validateToken(request.getAccessToken())) {
                tokenBlacklistService.blacklistToken(request.getAccessToken());
            }
        }

        // 리프레시 토큰이 제공된 경우 블랙리스트 추가
        if (request.getRefreshToken() != null && !request.getRefreshToken().trim().isEmpty()) {
            if (jwtTokenProvider.validateToken(request.getRefreshToken())) {
                tokenBlacklistService.blacklistToken(request.getRefreshToken());
            }
        }

        return new LogoutResponse(true, "로그아웃이 완료되었습니다");
    }
}
