package com.jiujitsu.api.domain.community.profile.mapper;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDivisionDto;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDto;
import com.jiujitsu.api.domain.community.profile.dto.MedalCountDto;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionInfo;
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

    // 대회정보 가져오기 - 한 줄(부문)씩 저장된 기록을 (연도, 대회명)으로 묶는다
    private List<CompetitionDto> mapCompetitions(CommunityProfile profile) {
        if (profile.getCompetitions() == null) {
            return Collections.emptyList();
        }

        Comparator<CompetitionRank> rankOrder = Comparator.nullsLast(Comparator.naturalOrder());
        Map<CompetitionKey, List<CompetitionInfo>> grouped = profile.getCompetitions().stream()
                .collect(Collectors.groupingBy(CompetitionKey::from, LinkedHashMap::new, Collectors.toList()));

        return grouped.entrySet().stream()
                .map(entry -> new CompetitionDto(
                        entry.getKey().year(),
                        entry.getKey().name(),
                        entry.getValue().stream()
                                // 같은 메달끼리는 도복/노기 → 띠 순 (저장 순서가 보장되지 않아 고정)
                                .sorted(Comparator.comparing(CompetitionInfo::getCompetitionRank, rankOrder)
                                        .thenComparing(CompetitionInfo::getUniform, Comparator.nullsLast(Comparator.naturalOrder()))
                                        .thenComparing(CompetitionInfo::getBelt, Comparator.nullsLast(Comparator.naturalOrder())))
                                .map(CompetitionDivisionDto::from)
                                .toList()
                ))
                // 연도 최신순 → 같은 연도는 최고 메달 순 → 대회명
                .sorted(Comparator.comparing(CompetitionDto::competitionYear, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(competition -> competition.divisions().get(0).competitionRank(), rankOrder)
                        .thenComparing(CompetitionDto::competitionName, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    // 메달 개수 합계 - 부문 기준, 참가 제외
    private MedalCountDto countMedals(CommunityProfile profile) {
        if (profile.getCompetitions() == null) {
            return new MedalCountDto(0, 0, 0);
        }

        Map<CompetitionRank, Long> counts = profile.getCompetitions().stream()
                .filter(competition -> competition.getCompetitionRank() != null)
                .collect(Collectors.groupingBy(CompetitionInfo::getCompetitionRank, Collectors.counting()));
        return new MedalCountDto(
                counts.getOrDefault(CompetitionRank.GOLD, 0L).intValue(),
                counts.getOrDefault(CompetitionRank.SILVER, 0L).intValue(),
                counts.getOrDefault(CompetitionRank.BRONZE, 0L).intValue()
        );
    }

    // 한 대회로 묶는 기준 - 대회명은 앞뒤 공백 무시
    private record CompetitionKey(Integer year, String name) {
        static CompetitionKey from(CompetitionInfo competition) {
            return new CompetitionKey(competition.getCompetitionYear(), StringUtils.trim(competition.getCompetitionName()));
        }
    }
}
