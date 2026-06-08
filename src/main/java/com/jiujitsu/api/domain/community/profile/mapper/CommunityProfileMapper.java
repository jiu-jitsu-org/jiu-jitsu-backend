package com.jiujitsu.api.domain.community.profile.mapper;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionInfoDto;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.OwnerProfile;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

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
                mapCompetitions(profile)
        );
    }

    /**
     * 프로필 응답 생성 - User 정보만
     */
    public CommunityProfileResponse getCommunityProfileResponseUser(User user) {
        return new CommunityProfileResponse(
                user.getNickname(),
                ImageInfo.from(user.getProfileImageFile()),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
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

    // 대회정보 가져오기
    private List<CompetitionInfoDto> mapCompetitions(CommunityProfile profile) {
        if (profile.getCompetitions() == null) {
            return Collections.emptyList();
        }

        return profile.getCompetitions().stream()
                .map(CompetitionInfoDto::new)
                .toList();
    }
}
