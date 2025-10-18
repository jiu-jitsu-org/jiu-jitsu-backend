package com.jiujitsu.api.domain.community.service;

import com.jiujitsu.api.domain.community.dto.CommunityProfileRequest;
import com.jiujitsu.api.domain.community.dto.CommunityProfileResponse;
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
        CommunityProfile profile = communityProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.ENTITY_NOT_FOUND));

        return new CommunityProfileResponse(profile);
    }

    public CommunityProfileResponse upsertMyProfile(CommunityProfileRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId();
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        CommunityProfile profile = communityProfileRepository.findByUser(user).orElseGet(CommunityProfile::new);
        profile.upsert(request, user);

        CommunityProfile saved = communityProfileRepository.save(profile);
        return new CommunityProfileResponse(saved);
    }
}
