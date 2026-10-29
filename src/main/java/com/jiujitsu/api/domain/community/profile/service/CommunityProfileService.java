package com.jiujitsu.api.domain.community.profile.service;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileRequest;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CommunityPublicProfileResponse;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.mapper.CommunityProfileMapper;
import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityProfileService {

    private final CommunityProfileRepository communityProfileRepository;
    private final UserRepository userRepository;
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
     * 다른 회원 공개 프로필 조회 (비로그인 허용, 차단 관계 무관)
     */
    @Transactional(readOnly = true)
    public CommunityPublicProfileResponse getPublicProfile(Long userId) {
        User user = userRepository.findById(userId)
                .filter(found -> found.getStatus() != UserStatus.DELETED)
                .orElseThrow(() -> new ErrorException(ErrorCode.MEMBER_NOT_FOUND));

        // 프로필 없으면 user 정보만 return
        CommunityProfile communityProfile = communityProfileRepository.findByUser(user).orElse(null);
        return communityProfileMapper.getCommunityPublicProfileResponse(user, communityProfile);
    }

    /**
     * 작성자 커뮤니티 프로필 일괄 조회 (userId → 프로필, 미작성 유저는 key 없음)
     */
    @Transactional(readOnly = true)
    public Map<Long, CommunityProfile> getProfilesByUserIds(Collection<Long> userIds) {
        // 빈 리스트일 때 IN 절 오류 방지
        if (userIds.isEmpty()) {
            return Map.of();
        }
        return communityProfileRepository.findAllByUserIdIn(userIds).stream()
                .collect(Collectors.toMap(profile -> profile.getUser().getId(), Function.identity(), (first, duplicate) -> first));
    }

    /**
     * 작성자 커뮤니티 프로필 단건 조회 (미작성 시 empty)
     */
    @Transactional(readOnly = true)
    public Optional<CommunityProfile> findProfile(User user) {
        return communityProfileRepository.findByUser(user);
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
