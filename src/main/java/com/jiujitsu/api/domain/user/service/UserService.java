package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.dto.*;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.security.JwtTokenProvider;
import com.jiujitsu.api.global.security.TokenBlacklistService;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthResponse createUser(CreateProfileRequest createProfileRequest) {
        String tempToken = createProfileRequest.getTempToken();
        String nickname = StringUtils.trimToEmpty(createProfileRequest.getNickname());

        // 임시 토큰 정보
        if (!jwtTokenProvider.validateToken(tempToken)) {
            throw new ErrorException(ErrorCode.INVALID_TOKEN);
        }
        if (!StringUtils.equals(jwtTokenProvider.getTokenTypeFromToken(tempToken), "temporary")) {
            throw new ErrorException(ErrorCode.NOT_MATCH_CATEGORY);
        }

        Claims claims = jwtTokenProvider.getJWTClaims(tempToken);
        String snsId = claims.getSubject();
        String email = claims.get("email", String.class);
        SnsProvider snsProvider = SnsProvider.valueOf(claims.get("snsProvider", String.class));

        // 닉네임 valid 체크
        String pattern = "^[가-힣a-zA-Z0-9]{2,12}$";
        if (!nickname.matches(pattern)) {
            throw new ErrorException(ErrorCode.NICKNAME_VALIDATION);
        }

        // 회원정보 생성
        User user = createNewUser(snsProvider, new SnsUserInfo(snsId, email));

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

    public UserProfileResponse getUserProfile() {
        // SecurityContext에서 인증된 사용자 ID 가져오기
        Long userId = AuthenticationUtil.getCurrentUserId();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        return new UserProfileResponse(user);
    }

    public UpdateProfileResponse updateProfile(UpdateProfileRequest request) {
        // SecurityContext에서 인증된 사용자 ID 가져오기
        Long userId = AuthenticationUtil.getCurrentUserId();

        // 사용자 조회
        User user = userRepository.findById(userId)
        .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        // 프로필 업데이트
        user.updateProfile(request.getNickname(), request.getProfileImageUrl());
        userRepository.save(user);

        return new UpdateProfileResponse(user);
    }

    public void deactivateUser() {
        // SecurityContext에서 인증된 사용자 ID 가져오기
        Long userId = AuthenticationUtil.getCurrentUserId();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        // 이미 탈퇴한 사용자인 경우
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ErrorException(ErrorCode.USER_ALREADY_DEACTIVATED);
        }

        // 현재 사용자의 JWT 토큰을 블랙리스트에 추가
        String token = AuthenticationUtil.getCurrentToken();
        if (token != null) {
            tokenBlacklistService.blacklistToken(token);
        }

        // 사용자 상태를 DELETED로 변경 (soft delete)
        user.updateStatus(UserStatus.DELETED);
        userRepository.save(user);
    }
    
    public boolean reactivateUserIfWithinGracePeriod(User user) {
        if (user.getStatus() == UserStatus.DELETED && user.isWithinGracePeriod()) {
            user.updateStatus(UserStatus.ACTIVE);
            userRepository.save(user);
            return true;
        }
        return false;
    }

    private User createNewUser(SnsProvider snsProvider, SnsUserInfo snsUserInfo) {
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
}