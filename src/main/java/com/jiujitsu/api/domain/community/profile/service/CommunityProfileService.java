package com.jiujitsu.api.domain.community.profile.service;

import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileRequest;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class CommunityProfileService {

    private final CommunityProfileRepository communityProfileRepository;
    private final UserRepository userRepository;

    /**
     * 커뮤니티 프로필 조회
     */
    @Transactional(readOnly = true)
    public CommunityProfileResponse getMyProfile() {
        Long userId = AuthenticationUtil.getCurrentUserId()
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));
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

    /**
     * 커뮤니티 프로필 등록/수정
     */
    public CommunityProfileResponse upsertMyProfile(CommunityProfileRequest request) {
        Long userId = AuthenticationUtil.getCurrentUserId()
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        CommunityProfile profile = communityProfileRepository.findByUser(user).orElse(new CommunityProfile(user));

        switch (request.getProfileRequestType()) {
            case ACADEMY -> profile.upsertAcademy(StringUtils.trimToEmpty(request.getAcademyName()));
            case BELT_WEIGHT -> profile.upsertBeltWeight(
                    request.getBeltRank(),
                    request.getBeltStripe(),
                    request.getGender(),
                    request.getWeightKg(),
                    request.getIsWeightHidden()
            );
            case POSITION_BEST -> profile.upsertBestPosition(request.getBestPosition());
            case POSITION_FAVORITE -> profile.upsertFavoritePosition(request.getFavoritePosition());
            case SUBMISSION_BEST -> profile.upsertBestSubmission(request.getBestSubmission());
            case SUBMISSION_FAVORITE -> profile.upsertFavoriteSubmission(request.getFavoriteSubmission());
            case TECHNIQUE_BEST -> profile.upsertBestTechnique(request.getBestTechnique());
            case TECHNIQUE_FAVORITE -> profile.upsertFavoriteTechnique(request.getFavoriteTechnique());
            case COMPETITION -> profile.upsertCompetitions(request.getCompetitionInfoList());
            case OWNER_INFO -> {
                if (Objects.equals(user.getRole(), UserRole.OWNER)) {
                    profile.upsertOwnerInfo(request.getTeachingPhilosophy(), request.getTeachingStartDate(), request.getTeachingDetail());
                }
            }
        }
        communityProfileRepository.save(profile);

        return new CommunityProfileResponse(profile);
    }
}
