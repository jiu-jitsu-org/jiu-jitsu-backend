package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.file.repository.ImageFileRepository;
import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import com.jiujitsu.api.domain.notice.repository.UserNoticeSettingRepository;
import com.jiujitsu.api.domain.user.dto.*;
import com.jiujitsu.api.domain.user.entity.*;
import com.jiujitsu.api.domain.user.factory.UserFactory;
import com.jiujitsu.api.domain.user.mapper.AuthMapper;
import com.jiujitsu.api.domain.user.mapper.UserMapper;
import com.jiujitsu.api.domain.user.repository.OwnerVerificationRequestRepository;
import com.jiujitsu.api.domain.user.repository.UserAppInfoRepository;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.security.JwtTokenProvider;
import com.jiujitsu.api.global.security.TokenBlacklistService;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final ImageFileRepository imageFileRepository;
    private final OwnerVerificationRequestRepository ownerVerificationRequestRepository;
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
        String accessToken = jwtTokenProvider.createAccessToken(user.getId(), user.getEmail(), user.getRole());
        String refreshToken = jwtTokenProvider.createRefreshToken(user.getId());

        UserInfo userInfo = userFactory.createUserInfo(user, false);

        return authMapper.toAccessResponse(accessToken, refreshToken, userInfo);
    }

    /**
     * 사용자 프로필 조회
     */
    public UserProfileResponse getUserProfile() {
        // 사용자 조회
        return toUserProfileResponse(authenticationFacade.getCurrentUser());
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

        return toUserProfileResponse(user);
    }

    /**
     * 사용자 프로필 이미지 수정
     */
    public UserProfileResponse updateProfileImage(Long imageFileId) {
        User user = authenticationFacade.getCurrentUser();
        ImageFile imageFile = imageFileRepository.findById(imageFileId)
                .orElseThrow(() -> new ErrorException(ErrorCode.IMAGE_FILE_NOT_FOUND));
        user.updateProfileImage(imageFile);
        return toUserProfileResponse(user);
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

    private UserProfileResponse toUserProfileResponse(User user) {
        OwnerVerificationRequest latestOwnerRequest = ownerVerificationRequestRepository.findTopByUserOrderByIdDesc(user)
                .orElse(null);
        return userMapper.toUserProfileResponse(user, latestOwnerRequest);
    }

    /**
     * 닉네임 유효성 체크
     */
    public void validateNickname(String nickname) {
        // 유효성 체크
        String pattern = "^[가-힣a-zA-Z0-9]{2,12}$";
        if (!nickname.matches(pattern)) {
            throw new ErrorException(ErrorCode.NICKNAME_VALIDATION);
        }
        // 중복 체크
        if (userRepository.findByNickname(nickname).isPresent()) {
            throw new ErrorException(ErrorCode.NICKNAME_DUPLICATED);
        }
    }

    /**
     * 앱 정보 등록
     */
    public void insertUserAppInfo(UserAppInfoRequest userAppInfoRequest) {
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
                userAppInfo.changeOsVersion(userAppInfoRequest.getOsVersion());
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
            userAppInfo.assignUser(user);

            userAppInfoRepository.save(userAppInfo);
        }
    }

    public List<UserAppInfoResponse> getUserAppInfo() {
        User user = authenticationFacade.getCurrentUser();

        return user.getAppInfos().stream()
                .map(info -> UserAppInfoResponse.toResponse(info, user.getNickname()))
                .toList();
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