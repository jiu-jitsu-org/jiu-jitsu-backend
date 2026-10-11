package com.jiujitsu.api.domain.community.profile.service;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileRequest;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.mapper.CommunityProfileMapper;
import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityProfileService {

    private final CommunityProfileRepository communityProfileRepository;
    private final AuthenticationFacade authenticationFacade;
    private final CommunityProfileMapper communityProfileMapper;
    private final UserRepository userRepository;
    private final UserBlockService userBlockService;

    /**
     * 다른 사용자 선수 카드 조회 (비로그인 가능)
     * - 차단 관계(내가 차단 / 나를 차단) 이면 404
     * - weightHidden=true 이면 체급 마스킹
     */
    @Transactional(readOnly = true)
    public CommunityProfileResponse getProfile(Long userId) {
        User target = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        authenticationFacade.getCurrentUserOptional().ifPresent(viewer -> {
            if (userBlockService.isBlockedBetween(viewer.getId(), userId)) {
                throw new ErrorException(ErrorCode.USER_NOT_FOUND);
            }
        });

        return communityProfileRepository.findByUser(target)
                .map(communityProfileMapper::getCommunityProfileResponseForOther)
                .orElseGet(() -> communityProfileMapper.getCommunityProfileResponseUser(target));
    }

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
                if (!Objects.equals(user.getRole(), UserRole.OWNER)) {
                    throw new ErrorException(ErrorCode.PERMISSION_DENIED);
                }
                profile.upsertOwnerInfo(request.teachingPhilosophy(), request.teachingStartDate(), request.teachingDetail());
            }
        }
    }
}
