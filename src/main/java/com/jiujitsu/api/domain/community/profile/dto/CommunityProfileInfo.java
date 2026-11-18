package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.community.profile.entity.BeltRank;
import com.jiujitsu.api.domain.community.profile.entity.BeltStripe;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.entity.AccountType;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import io.swagger.v3.oas.annotations.media.Schema;

public record CommunityProfileInfo(
        Long id,
        String nickname,
        ImageInfo profileImage,
        @Schema(description = "벨트 등급 (미입력 시 null)", example = "BLUE") BeltRank beltRank,
        @Schema(description = "그랄 (미입력 시 null)", example = "STRIPE_2") BeltStripe beltStripe,
        @Schema(description = "관장/사범 인증 여부") Boolean isOwner,
        @Schema(description = "운영자 여부 (계정 구분 OPERATOR — OSS 공식 · 운영자 계정, 권한과 무관). 닉네임 옆 '운영자' 배지") Boolean isOperator
) {

    /**
     * 작성자 정보 - profile 은 커뮤니티 프로필 미작성 시 null
     */
    public static CommunityProfileInfo from(User user, CommunityProfile profile) {
        if (user == null) {
            return withdrawn();
        }
        return new CommunityProfileInfo(
                user.getId(),
                user.getNickname(),
                ImageInfo.from(user.getProfileImageFile()),
                profile != null ? profile.getBeltRank() : null,
                profile != null ? profile.getBeltStripe() : null,
                user.getRole() == UserRole.OWNER,
                user.getAccountType() == AccountType.OPERATOR
        );
    }

    public static CommunityProfileInfo withdrawn() {
        return new CommunityProfileInfo(null, "탈퇴회원", null, null, null, null, null);
    }

    public static CommunityProfileInfo masked() {
        return new CommunityProfileInfo(null, null, null, null, null, null, null);
    }
}
