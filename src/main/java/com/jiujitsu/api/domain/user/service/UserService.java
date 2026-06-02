package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.OwnerProfile;
import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.community.profile.repository.OwnerProfileRepository;
import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import com.jiujitsu.api.domain.notice.repository.UserNoticeSettingRepository;
import com.jiujitsu.api.domain.user.dto.*;
import com.jiujitsu.api.domain.user.entity.*;
import com.jiujitsu.api.domain.user.factory.UserFactory;
import com.jiujitsu.api.domain.user.mapper.AuthMapper;
import com.jiujitsu.api.domain.user.mapper.UserMapper;
import com.jiujitsu.api.domain.user.repository.UserAppInfoRepository;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.security.JwtTokenProvider;
import com.jiujitsu.api.global.security.TokenBlacklistService;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final OwnerProfileRepository ownerProfileRepository;
    private final CommunityProfileRepository communityProfileRepository;
    private final UserAppInfoRepository userAppInfoRepository;
    private final UserNoticeSettingRepository userNoticeSettingRepository;
    private final TokenBlacklistService tokenBlacklistService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthenticationFacade authenticationFacade;
    private final AuthMapper authMapper;
    private final UserFactory userFactory;
    private final UserMapper userMapper;

    /**
     * 회원가입 - 로직 분리
     */
    public AuthResponse createUser(CreateProfileRequest createProfileRequest) {
        // 임시 유저 정보
        String tempToken = AuthenticationUtil.getCurrentToken()
                .orElseThrow(() -> new ErrorException(ErrorCode.NO_TOKEN));

        TempUserInfo tempUserInfo = jwtTokenProvider.parseTemporaryToken(tempToken);

        // 닉네임 valid 체크
        String nickname = StringUtils.trimToEmpty(createProfileRequest.nickname());
        validateNickname(nickname);


        // 회원정보 생성
        User user = createNewUser(tempUserInfo, nickname);

        // 응답 생성(토큰 생성)
        return generateAuthResponse(user);
    }

    /**
     * 회원가입 - 신규 user 생성
     */
    private User createNewUser(TempUserInfo tempUserInfo, String nickname) {
        User user = userFactory.createNewUser(
                tempUserInfo.snsProvider(),
                new SnsUserInfo(
                        tempUserInfo.snsId(),
                        tempUserInfo.email(),
                        nickname
                )
        );

        UserNoticeSetting userNoticeSetting = userNoticeSettingRepository.save(new UserNoticeSetting(user));
        user.setUserNoticeSetting(userNoticeSetting);
        return userRepository.save(user);
    }

    /**
     * 회원가입 - 로그인 토큰 / AuthResponse 생성
     */
    private AuthResponse generateAuthResponse(User user) {
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        UserInfo userInfo = userFactory.createUserInfo(user, false);

        return authMapper.toAccessResponse(accessToken, refreshToken, userInfo);
    }

    /**
     * 사용자 프로필 조회
     */
    public UserProfileResponse getUserProfile() {
        // 사용자 조회
        return userMapper.toUserProfileResponse(authenticationFacade.getCurrentUser());
    }

    /**
     * 사용자 닉네임 수정
     */
    public UserProfileResponse updateProfileNickname(String nickname) {
        // 사용자 조회
        User user = authenticationFacade.getCurrentUser();

        // 닉네임 중복체크
        validateNickname(nickname);
        // 프로필 업데이트
        user.updateNickname(nickname);

        return userMapper.toUserProfileResponse(user);
    }

    /**
     * 사용자 프로필 이미지 수정
     */
    public UserProfileResponse updateProfileImage(String profileImageUrl) {
        // 사용자 조회
        User user = authenticationFacade.getCurrentUser();
        user.updateProfileImage(profileImageUrl);

        return userMapper.toUserProfileResponse(user);
    }

    /**
     * 회원 비활성화
     */
    public void deactivateUser() {
        // 사용자 조회
        User user = authenticationFacade.getCurrentUser();

        // 이미 탈퇴한 사용자인 경우
        if (user.getStatus() == UserStatus.DELETED) {
            throw new ErrorException(ErrorCode.USER_ALREADY_DEACTIVATED);
        }

        // 현재 사용자의 JWT 토큰을 블랙리스트에 추가
        String token = AuthenticationUtil.getCurrentToken()
                .orElseThrow(() -> new ErrorException(ErrorCode.NO_TOKEN));

        tokenBlacklistService.blacklistToken(token);

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
        // 사용자 조회
        User user = authenticationFacade.getCurrentUser();

        // 신청 권한체크
        if (!Objects.equals(user.getRole(), UserRole.USER)) {
            throw new ErrorException(ErrorCode.PERMISSION_DENIED);
        }

        // 사용자 권한 변경
        user.requestOwner(ownerRequestImageUrl);

        return userMapper.toUserProfileResponse(user);
    }

    /**
     * 관장/사범 목록 조회
     */
    public List<UserProfileResponse> getRequestOwner() {
        List<User> userList = userRepository.findByOwnerRequestedTrueAndOwnerRequestImageUrlIsNotNullAndRole(UserRole.USER);
        return userList.stream().map(userMapper::toUserProfileResponse).toList();
    }

    /**
     * 관장/사범 권한 부여
     */
    public UserProfileResponse grantOwnerRole() {
        // 사용자 조회
        User user = authenticationFacade.getCurrentUser();

        // 사용자 권한 변경
        user.updateRole(UserRole.OWNER);

        // 관장 프로필 생성
        OwnerProfile ownerProfile = OwnerProfile.builder()
                .documentImageUrl("defaultDocument")
                .build();
        ownerProfileRepository.save(ownerProfile);

        // 관장-커뮤 프로필 매핑
        CommunityProfile communityProfile = communityProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ErrorException(ErrorCode.REQUIRED_PROFILE));
        communityProfile.insertOwnerProfile(ownerProfile);

        return userMapper.toUserProfileResponse(user);
    }

    /**
     * 닉네임 유효성 체크
     */
    public Boolean validateNickname(String nickname) {
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

    /**
     * 앱 정보 등록
     */
    public Boolean insertUserAppInfo(UserAppInfoRequest userAppInfoRequest) {
        // 사용자 조회
        User user = authenticationFacade.getCurrentUser();

        List<UserAppInfo> userAppInfos = user.getAppInfos();
        Optional<UserAppInfo> userAppInfoOptional = userAppInfos.stream()
                .filter(info -> Objects.equals(userAppInfoRequest.getFcmToken(), info.getToken()))
                .findFirst();

        if (userAppInfoOptional.isPresent()) {
            // 동일한 fcm 토큰 있는 경우
            UserAppInfo userAppInfo = userAppInfoOptional.get();
            if (!Objects.equals(userAppInfo.getOsVersion(), userAppInfoRequest.getOsVersion())) {
                userAppInfo.setOsVersion(userAppInfo.getOsVersion());
                userAppInfoRepository.save(userAppInfo);
            }
        } else {
            // deviceId 중복된 데이터 삭제
            List<UserAppInfo> duplicateDevice = userAppInfos.stream()
                    .filter(info -> Objects.equals(info.getDeviceId(), userAppInfoRequest.getDeviceId()))
                    .toList();

            userAppInfoRepository.deleteAll(duplicateDevice);

            // 동일한 fcm 토큰 없는 경우
            UserAppInfo userAppInfo = userAppInfoRequest.toEntity();
            userAppInfo.setUser(user);

            userAppInfoRepository.save(userAppInfo);
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

    public List<UserAppInfoResponse> getUserAppInfo() {
        User user = authenticationFacade.getCurrentUser();
        List<UserAppInfoResponse> result = new ArrayList<>();
        for (UserAppInfo appInfo : user.getAppInfos()) {
            result.add(
                    UserAppInfoResponse.toResponse(appInfo, user.getNickname())
            );
        }

        return result;
    }

    public List<UserAppInfoResponse> getUserAppInfoByNickname(String nickname) {
        User user = userRepository.findByNickname(StringUtils.trimToEmpty(nickname))
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));
        List<UserAppInfoResponse> result = new ArrayList<>();
        for (UserAppInfo appInfo : user.getAppInfos()) {
            result.add(
                    UserAppInfoResponse.toResponse(appInfo, user.getNickname())
            );
        }

        return result;
    }
}