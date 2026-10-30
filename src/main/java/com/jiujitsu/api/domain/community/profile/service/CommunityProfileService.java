package com.jiujitsu.api.domain.community.profile.service;

import com.jiujitsu.api.domain.community.competition.entity.Competition;
import com.jiujitsu.api.domain.community.competition.service.CompetitionService;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileRequest;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CommunityPublicProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDto;
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

import java.time.Year;
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
    private final CompetitionService competitionService;

    private static final int MAX_PINNED_COMPETITIONS = 3;

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
            case BELT -> profile.upsertBelt(request.beltRank(), request.beltStripe());
            case POSITION -> profile.upsertPosition(request.bestPosition(), request.favoritePosition());
            case SUBMISSION -> profile.upsertSubmission(request.bestSubmission(), request.favoriteSubmission());
            case TECHNIQUE -> profile.upsertTechnique(request.bestTechnique(), request.favoriteTechnique());
            case COMPETITION -> {
                // 필드가 빠진 요청으로 기록이 통째로 지워지지 않게 막는다. 전체 삭제는 빈 목록으로
                if (request.competitions() == null) {
                    throw new ErrorException(ErrorCode.REQUIRED_PARAMETER);
                }
                // 등록 대회를 고른 기록은 대회명·연도를 등록 대회 값으로 맞춘 뒤 검사한다 (중복·고정 규칙이 같은 기준으로 걸리게)
                Set<Long> competitionIds = request.competitions().stream()
                        .map(CompetitionDto::competitionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
                Map<Long, Competition> registered = competitionIds.isEmpty()
                        ? Map.of()
                        : competitionService.getCompetitionsByIds(competitionIds);
                List<CompetitionDto> competitions = request.competitions().stream()
                        .map(competition -> competition.competitionId() == null
                                ? competition
                                : competition.withRegistered(registered.get(competition.competitionId())))
                        .toList();
                validateCompetitions(competitions);
                profile.upsertCompetitions(competitions, registered);
            }
            case OWNER_INFO -> {
                if (!Objects.equals(user.getRole(), UserRole.OWNER)) {
                    throw new ErrorException(ErrorCode.PERMISSION_DENIED);
                }
                profile.upsertOwnerInfo(request.teachingPhilosophy(), request.teachingStartDate(), request.teachingDetail());
            }
        }
    }

    // 대표 고정은 최대 3개, 이름 있는 같은 대회(연도+대회명)는 한 번만 - 대회명 없는 기록은 각각 별개
    private void validateCompetitions(List<CompetitionDto> competitions) {
        Set<String> competitionKeys = new HashSet<>();
        boolean duplicated = competitions.stream()
                .filter(CompetitionDto::named)
                .anyMatch(competition -> !competitionKeys.add(competition.competitionYear() + "|" + competition.trimmedName()));
        if (duplicated) {
            throw new ErrorException(ErrorCode.COMPETITION_DUPLICATED);
        }

        // 등록 대회 연도가 올해 이후면(예정 대회) 기록으로 받지 않는다 - 요청 연도는 DTO 에서 이미 검사
        if (competitions.stream().anyMatch(competition -> competition.competitionYear() > Year.now().getValue())) {
            throw new ErrorException(ErrorCode.WRONG_PARAMETER);
        }

        long pinnedCount = competitions.stream().filter(CompetitionDto::pinnedOrFalse).count();
        if (pinnedCount > MAX_PINNED_COMPETITIONS) {
            throw new ErrorException(ErrorCode.COMPETITION_PIN_LIMIT_EXCEEDED);
        }
    }
}
