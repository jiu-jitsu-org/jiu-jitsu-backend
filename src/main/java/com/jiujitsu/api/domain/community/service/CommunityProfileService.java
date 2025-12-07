package com.jiujitsu.api.domain.community.service;

import com.jiujitsu.api.domain.community.dto.*;
import com.jiujitsu.api.domain.community.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommunityProfileService {

    private final CommunityProfileRepository communityProfileRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public CommunityProfileResponse getMyProfile() {
        Long userId = AuthenticationUtil.getCurrentUserId();
        Optional<CommunityProfile> profile = communityProfileRepository.findByUserId(userId);

        // 없는 경우 null return
        if (profile.isEmpty()) {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

            CommunityProfileResponse communityProfile = new CommunityProfileResponse();
            communityProfile.setNickname(user.getNickname());
            communityProfile.setProfileImageUrl(user.getProfileImageUrl());
            return communityProfile;
        }

        return new CommunityProfileResponse(profile.get());
    }

    public CommunityProfileResponse upsertMyProfile(CommunityProfileRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        CommunityProfile profile = communityProfileRepository.findByUser(user).orElseGet(CommunityProfile::new);
        profile.upsert(request, user);
        communityProfileRepository.save(profile);

        return new CommunityProfileResponse(profile);
    }

    public CommunityProfileResponse upsertAcademyInfo(AcademyUpdateRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        CommunityProfile profile = communityProfileRepository.findByUser(user).orElseGet(CommunityProfile::new);
        profile.upsertAcademyInfo(request, user);
        communityProfileRepository.save(profile);

        return new CommunityProfileResponse(profile);
    }

    public CommunityProfileResponse upsertLevelInfo(LevelUpdateRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        CommunityProfile profile = communityProfileRepository.findByUser(user).orElseGet(CommunityProfile::new);
        profile.upsertLevelInfo(request, user);
        communityProfileRepository.save(profile);

        return new CommunityProfileResponse(profile);
    }

    public CommunityProfileResponse upsertTechniqueInfo(TechniqueUpdateRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        CommunityProfile profile = communityProfileRepository.findByUser(user).orElseGet(CommunityProfile::new);
        profile.upsertTechniqueInfo(request, user);
        communityProfileRepository.save(profile);

        return new CommunityProfileResponse(profile);
    }
}
