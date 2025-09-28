package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.dto.UpdateProfileRequest;
import com.jiujitsu.api.domain.user.dto.UpdateProfileResponse;
import com.jiujitsu.api.domain.user.dto.UserProfileResponse;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.security.TokenBlacklistService;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final TokenBlacklistService tokenBlacklistService;

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

}