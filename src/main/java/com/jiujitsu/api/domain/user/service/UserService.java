package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.community.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.entity.OwnerProfile;
import com.jiujitsu.api.domain.community.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.community.repository.OwnerProfileRepository;
import com.jiujitsu.api.domain.user.dto.*;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
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
    private final OwnerProfileRepository ownerProfileRepository;
    private final CommunityProfileRepository communityProfileRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 회원가입
     */
    public AuthResponse createUser(CreateProfileRequest createProfileRequest) {
        String tempToken = AuthenticationUtil.getCurrentToken();
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
        this.checkNickname(nickname);

        // 회원정보 생성
        User user = createNewUser(snsProvider, new SnsUserInfo(snsId, email, nickname));

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

    /**
     * 사용자 프로필 조회
     */
    public UserProfileResponse getUserProfile() {
        // SecurityContext에서 인증된 사용자 ID 가져오기
        Long userId = AuthenticationUtil.getCurrentUserId();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        return new UserProfileResponse(user);
    }

    /**
     * 사용자 프로필 수정
     */
    public UpdateProfileResponse updateProfile(UpdateProfileRequest request) {
        // SecurityContext에서 인증된 사용자 ID 가져오기
        final Long userId = AuthenticationUtil.getCurrentUserId();
        final String nickname = StringUtils.trimToEmpty(request.getNickname());
        final String profileImageUrl = StringUtils.trimToEmpty(request.getProfileImageUrl());

        // 사용자 조회
        User user = userRepository.findById(userId)
        .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        // 닉네임 중복체크
        checkNickname(nickname);
        // 프로필 업데이트
        user.updateProfile(nickname, profileImageUrl);

        return new UpdateProfileResponse(user);
    }

    /**
     * 회원 비활성화
     */
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
    }

    /**
     * 회원 활성화
     */
    public boolean reactivateUserIfWithinGracePeriod(User user) {
        if (user.getStatus() == UserStatus.DELETED && user.isWithinGracePeriod()) {
            user.updateStatus(UserStatus.ACTIVE);
            return true;
        }
        return false;
    }

    /**
     * 관장/사범 신청
     */
    public UserProfileResponse requestOwnerRole(String ownerRequestImageUrl) {
        // SecurityContext에서 인증된 사용자 ID 가져오기
        Long userId = AuthenticationUtil.getCurrentUserId();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        // 사용자 권한 변경
        user.requestOwner(ownerRequestImageUrl);

        return new UserProfileResponse(user);
    }

    /**
     * 관장/사범 권한 부여
     */
    public UserProfileResponse grantOwnerRole() {
        // SecurityContext에서 인증된 사용자 ID 가져오기
        Long userId = AuthenticationUtil.getCurrentUserId();

        // 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        // 사용자 권한 변경
        user.updateRole(UserRole.OWNER);

        // 관장 프로필 생성
        OwnerProfile ownerProfile = OwnerProfile.builder()
                .documentImageUrl("defaultDocument")
                .build();
        ownerProfileRepository.save(ownerProfile);

        // 관장-커뮤 프로필 매핑
        CommunityProfile communityProfile = communityProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.REQUIRED_PROFILE));
        communityProfile.insertOwnerProfile(ownerProfile);

        return new UserProfileResponse(user);
    }

    /**
     * 닉네임 유효성 체크
     */
    public Boolean checkNickname(String nickname) {
        // 유효성 체크
        String pattern = "^[가-힣a-zA-Z0-9]{2,12}$";
        if (!nickname.matches(pattern)) {
            throw new ErrorException(ErrorCode.NICKNAME_VALIDATION);
        }
        // 중복 체크
        if (userRepository.findByNickname(nickname).isPresent()) {
            throw new ErrorException(ErrorCode.NICKNAME_DUPLICATED);
        }
        return true;
    }

    private User createNewUser(SnsProvider snsProvider, SnsUserInfo snsUserInfo) {
        User newUser = User.builder()
                .email(snsUserInfo.getEmail())
                .nickname(snsUserInfo.getNickname())
                .profileImageUrl("default")
                .snsProvider(snsProvider)
                .snsId(snsUserInfo.getSnsId())
                .ownerRequested(false)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        return userRepository.save(newUser);
    }
}