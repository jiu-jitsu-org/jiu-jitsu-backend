package com.jiujitsu.api.domain.community.profile.mapper;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CommunityPublicProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDivisionDto;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDto;
import com.jiujitsu.api.domain.community.profile.dto.MedalCountDto;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionDivision;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionRecord;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionRank;
import com.jiujitsu.api.domain.community.profile.entity.OwnerProfile;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class CommunityProfileMapper {

    // 메달 높은 순 (GOLD → PARTICIPATION)
    private static final Comparator<CompetitionRank> RANK_ORDER = Comparator.nullsLast(Comparator.naturalOrder());

    /**
     * 프로필 응답 생성
     */
    public CommunityProfileResponse getCommunityProfileResponse(CommunityProfile profile) {
        User user = profile.getUser();
        boolean isOwner = user.getRole() == UserRole.OWNER;
        OwnerProfile ownerProfile = isOwner ? profile.getOwnerProfile() : null;

        return new CommunityProfileResponse(
                user.getNickname(),
                ImageInfo.from(user.getProfileImageFile()),
                profile.getBeltRank(),
                profile.getBeltStripe(),
                profile.getGender(),
                profile.getWeightKg(),
                profile.getAcademyName(),
                profile.getBestSubmission(),
                profile.getFavoriteSubmission(),
                profile.getBestTechnique(),
                profile.getFavoriteTechnique(),
                profile.getBestPosition(),
                profile.getFavoritePosition(),
                profile.getWeightHidden(),
                isOwner,
                getTeachingPhilosophy(ownerProfile),
                getTeachingStartDate(ownerProfile),
                getTeachingDetail(ownerProfile),
                mapCompetitions(profile),
                countMedals(profile)
        );
    }

    /**
     * 프로필 응답 생성 - User 정보만
     */
    public CommunityProfileResponse getCommunityProfileResponseUser(User user) {
        return CommunityProfileResponse.ofUserOnly(user);
    }

    /**
     * 공개 프로필 응답 생성 - profile 은 커뮤니티 프로필 미작성 시 null
     */
    public CommunityPublicProfileResponse getCommunityPublicProfileResponse(User user, CommunityProfile profile) {
        boolean isOwner = user.getRole() == UserRole.OWNER;
        if (profile == null) {
            return new CommunityPublicProfileResponse(
                    user.getId(),
                    user.getNickname(),
                    ImageInfo.from(user.getProfileImageFile()),
                    null, null, null,
                    isOwner,
                    null, null, null,
                    Collections.emptyList(),
                    new MedalCountDto(0, 0, 0)
            );
        }

        return new CommunityPublicProfileResponse(
                user.getId(),
                user.getNickname(),
                ImageInfo.from(user.getProfileImageFile()),
                StringUtils.trimToNull(profile.getAcademyName()),
                profile.getBeltRank(),
                profile.getBeltStripe(),
                isOwner,
                profile.getFavoritePosition(),
                profile.getFavoriteTechnique(),
                profile.getFavoriteSubmission(),
                mapPublicCompetitions(profile),
                countMedals(profile)
        );
    }

    //Owner 프로필 가져오기
    private String getTeachingPhilosophy(OwnerProfile ownerProfile) {
        return ownerProfile != null ? ownerProfile.getTeachingPhilosophy() : null;
    }
    private LocalDate getTeachingStartDate(OwnerProfile ownerProfile) {
        return ownerProfile != null ? ownerProfile.getTeachingStartDate() : null;
    }
    private String getTeachingDetail(OwnerProfile ownerProfile) {
        return ownerProfile != null ? ownerProfile.getTeachingDetail() : null;
    }

    // 대회정보 가져오기 - 고정 → 회원이 정한 순서 → 연도 최신순 → 최고 메달 → 대회명
    private List<CompetitionDto> mapCompetitions(CommunityProfile profile) {
        if (profile.getCompetitions() == null) {
            return Collections.emptyList();
        }

        return profile.getCompetitions().stream()
                .sorted(Comparator.comparing(CompetitionRecord::isCompetitionPinned).reversed()
                        .thenComparing(CompetitionRecord::getCompetitionDisplayOrder, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CompetitionRecord::getCompetitionYear, Comparator.reverseOrder())
                        .thenComparing(this::bestRank, RANK_ORDER)
                        .thenComparing(CompetitionRecord::getCompetitionName)
                        .thenComparing(CompetitionRecord::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(record -> CompetitionDto.from(record, mapDivisions(record)))
                .toList();
    }

    // 공개 대회정보 가져오기 - 대회명 없는 기록은 제외 (메달 합계와 같은 기준)
    private List<CompetitionDto> mapPublicCompetitions(CommunityProfile profile) {
        return mapCompetitions(profile).stream()
                .filter(CompetitionDto::named)
                .toList();
    }

    // 부문 - 메달 → 도복/노기 → 띠 → 체급(앱솔루트는 마지막) → 저장 순서
    private List<CompetitionDivisionDto> mapDivisions(CompetitionRecord record) {
        return record.getDivisions().stream()
                .sorted(Comparator.comparing(CompetitionDivision::getCompetitionRank, RANK_ORDER)
                        .thenComparing(CompetitionDivision::getCompetitionUniform, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CompetitionDivision::getCompetitionBelt, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CompetitionDivision::isCompetitionAbsolute)
                        .thenComparing(CompetitionDivision::getCompetitionWeightLimit, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(CompetitionDivision::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .map(CompetitionDivisionDto::from)
                .toList();
    }

    private CompetitionRank bestRank(CompetitionRecord record) {
        return record.getDivisions().stream()
                .map(CompetitionDivision::getCompetitionRank)
                .min(RANK_ORDER)
                .orElse(null);
    }

    // 메달 개수 합계 - 부문 기준, 참가·대회명 없는 기록 제외
    private MedalCountDto countMedals(CommunityProfile profile) {
        if (profile.getCompetitions() == null) {
            return new MedalCountDto(0, 0, 0);
        }

        Map<CompetitionRank, Long> counts = profile.getCompetitions().stream()
                .filter(CompetitionRecord::isNamed)
                .flatMap(record -> record.getDivisions().stream())
                .collect(Collectors.groupingBy(CompetitionDivision::getCompetitionRank, Collectors.counting()));
        return new MedalCountDto(
                counts.getOrDefault(CompetitionRank.GOLD, 0L).intValue(),
                counts.getOrDefault(CompetitionRank.SILVER, 0L).intValue(),
                counts.getOrDefault(CompetitionRank.BRONZE, 0L).intValue()
        );
    }
}
