package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.community.profile.entity.BeltRank;
import com.jiujitsu.api.domain.community.profile.entity.BeltStripe;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityProfileInfo(
        Long id,
        String nickname,
        ImageInfo profileImage,
        @Schema(description = "벨트 등급 (미입력 시 null)", example = "BLUE") BeltRank beltRank,
        @Schema(description = "그랄 (미입력 시 null)", example = "STRIPE_2") BeltStripe beltStripe,
        @Schema(description = "관장/사범 인증 여부") Boolean isOwner
) {
    /**
     * 작성자 정보 - profile 은 커뮤니티 프로필 미작성 시 null
     */
    public static CommunityProfileInfo from(User user, CommunityProfile profile) {
        return new CommunityProfileInfo(
                user.getId(),
                user.getNickname(),
                ImageInfo.from(user.getProfileImageFile()),
                profile != null ? profile.getBeltRank() : null,
                profile != null ? profile.getBeltStripe() : null,
                user.getRole() == UserRole.OWNER
        );
    }

    public static CommunityProfileInfo masked() {
        return new CommunityProfileInfo(null, null, null, null, null, null);
    }
}
