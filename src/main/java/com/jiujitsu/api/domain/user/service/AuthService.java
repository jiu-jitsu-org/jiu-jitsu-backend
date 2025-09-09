package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.dto.AuthResponse;
import com.jiujitsu.api.domain.user.dto.SnsLoginRequest;
import com.jiujitsu.api.domain.user.dto.SnsUserInfo;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.sns.SnsClient;
import com.jiujitsu.api.domain.user.service.sns.SnsClientFactory;
import com.jiujitsu.api.global.security.JwtTokenProvider;
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

    public AuthResponse snsLogin(SnsLoginRequest request) {
        try {
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

        } catch (Exception e) {
            log.error("SNS 로그인 처리 실패: {}", e.getMessage(), e);
            throw new RuntimeException("로그인 처리 중 오류가 발생했습니다", e);
        }
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
        try {
            // 리프레시 토큰 검증
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                throw new RuntimeException("유효하지 않은 리프레시 토큰입니다");
            }

            // 토큰 타입 확인
            String tokenType = jwtTokenProvider.getTokenTypeFromToken(refreshToken);
            if (!"refresh".equals(tokenType)) {
                throw new RuntimeException("리프레시 토큰이 아닙니다");
            }

            // 사용자 정보 조회
            Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

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

        } catch (Exception e) {
            log.error("토큰 갱신 실패: {}", e.getMessage(), e);
            throw new RuntimeException("토큰 갱신 중 오류가 발생했습니다", e);
        }
    }
}
