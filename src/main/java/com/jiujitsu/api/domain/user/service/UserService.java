package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.dto.UpdateProfileRequest;
import com.jiujitsu.api.domain.user.dto.UpdateProfileResponse;
import com.jiujitsu.api.domain.user.dto.UserProfileResponse;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.security.JwtTokenProvider;
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
    private final JwtTokenProvider jwtTokenProvider;

    public UserProfileResponse getUserProfile(String token) {
        // Bearer 접두사 제거
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 토큰에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        return new UserProfileResponse(user);
    }

    public UpdateProfileResponse updateProfile(String token, UpdateProfileRequest request) {
        // Bearer 접두사 제거
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }

        // 토큰에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(token);

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        // 프로필 업데이트
        user.updateProfile(request.getNickname(), request.getProfileImageUrl());
        userRepository.save(user);

        return new UpdateProfileResponse(user);
    }
}
