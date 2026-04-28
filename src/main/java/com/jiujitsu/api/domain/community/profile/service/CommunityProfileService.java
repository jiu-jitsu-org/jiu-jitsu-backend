package com.jiujitsu.api.domain.community.profile.service;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileRequest;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.mapper.CommunityProfileMapper;
import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
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
    private final AuthenticationFacade authenticationFacade;
    private final CommunityProfileMapper communityProfileMapper;

    /**
     * 커뮤니티 프로필 조회
     */
    @Transactional(readOnly = true)
    public CommunityProfileResponse getMyProfile() {
        User user = authenticationFacade.getCurrentUser();
        Optional<CommunityProfile> communityProfile = communityProfileRepository.findByUser(user);

        // 프로필 있으면 return, 없으면 user 정보만 return
        return communityProfile
                .map(communityProfileMapper::getCommunityProfileResponse)
                .orElseGet(() -> communityProfileMapper.getCommunityProfileResponseUser(user));
    }

    /**
     * 커뮤니티 프로필 등록/수정
     */
    public CommunityProfileResponse upsertMyProfile(CommunityProfileRequest request) {
        User user = authenticationFacade.getCurrentUser();

        CommunityProfile profile = communityProfileRepository.findByUser(user)
                .orElse(new CommunityProfile(user));

        // 각 항목별 업데이트 처리
        applyProfileUpdate(profile, request, user);

        // save 명시(신규 생성일 수 있음)
        communityProfileRepository.save(profile);

        // return 생성
        return communityProfileMapper.getCommunityProfileResponse(profile);
    }

    // 항목별 업데이트 처리
    private void applyProfileUpdate(CommunityProfile profile, CommunityProfileRequest request, User user) {
        switch (request.profileRequestType()) {
            case ACADEMY -> profile.upsertAcademy(StringUtils.trimToEmpty(request.academyName()));
            case BELT_WEIGHT -> profile.upsertBeltWeight(
                    request.beltRank(),
                    request.beltStripe(),
                    request.gender(),
                    request.weightKg(),
                    request.isWeightHidden()
            );
            case POSITION -> profile.upsertPosition(request.bestPosition(), request.favoritePosition());
            case SUBMISSION -> profile.upsertSubmission(request.bestSubmission(), request.favoriteSubmission());
            case TECHNIQUE -> profile.upsertTechnique(request.bestTechnique(), request.favoriteTechnique());
            case COMPETITION -> profile.upsertCompetitions(request.competitionInfoListOrEmpty());
            case OWNER_INFO -> {
                if (Objects.equals(user.getRole(), UserRole.OWNER)) {
                    profile.upsertOwnerInfo(request.teachingPhilosophy(), request.teachingStartDate(), request.teachingDetail());
                }
            }
        }
    }
}
