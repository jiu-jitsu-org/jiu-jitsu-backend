package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.dto.UpdateProfileRequest;
import com.jiujitsu.api.domain.user.dto.UpdateProfileResponse;
import com.jiujitsu.api.domain.user.dto.UserProfileResponse;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
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
        try {
            // Bearer 접두사 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 토큰에서 사용자 ID 추출
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            return new UserProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getSnsProvider().name(),
                    user.getRole().name(),
                    user.getStatus().name()
            );

        } catch (Exception e) {
            log.error("사용자 프로필 조회 실패: {}", e.getMessage(), e);
            throw new RuntimeException("사용자 프로필 조회 중 오류가 발생했습니다", e);
        }
    }

    public UpdateProfileResponse updateProfile(String token, UpdateProfileRequest request) {
        try {
            // Bearer 접두사 제거
            if (token.startsWith("Bearer ")) {
                token = token.substring(7);
            }

            // 토큰에서 사용자 ID 추출
            Long userId = jwtTokenProvider.getUserIdFromToken(token);
            
            // 사용자 조회
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("사용자를 찾을 수 없습니다"));

            // 프로필 업데이트
            user.updateProfile(request.getNickname(), request.getProfileImageUrl());
            userRepository.save(user);

            // 응답 생성
            UpdateProfileResponse.UserInfo userInfo = new UpdateProfileResponse.UserInfo(
                    user.getId(),
                    user.getEmail(),
                    user.getNickname(),
                    user.getProfileImageUrl(),
                    user.getSnsProvider().name()
            );

            log.info("사용자 프로필 업데이트 완료: userId={}", userId);
            return new UpdateProfileResponse(true, "프로필이 성공적으로 업데이트되었습니다", userInfo);

        } catch (Exception e) {
            log.error("프로필 업데이트 실패: {}", e.getMessage(), e);
            return new UpdateProfileResponse(false, "프로필 업데이트 중 오류가 발생했습니다", null);
        }
    }
}
