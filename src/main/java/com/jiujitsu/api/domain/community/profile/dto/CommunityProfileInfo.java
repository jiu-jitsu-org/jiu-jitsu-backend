package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.entity.User;

public record CommunityProfileInfo(
        Long id,
        String nickname,
        ImageInfo profileImage
) {
    public static CommunityProfileInfo from(User user) {
        if (user == null) {
            return withdrawn();
        }
        return new CommunityProfileInfo(
                user.getId(),
                user.getNickname(),
                ImageInfo.from(user.getProfileImageFile())
        );
    }

    public static CommunityProfileInfo withdrawn() {
        return new CommunityProfileInfo(null, "탈퇴회원", null);
    }

    public static CommunityProfileInfo masked() {
        return new CommunityProfileInfo(null, null, null);
    }
}
