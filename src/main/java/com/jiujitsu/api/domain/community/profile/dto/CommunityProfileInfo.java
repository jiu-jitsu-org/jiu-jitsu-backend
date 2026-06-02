package com.jiujitsu.api.domain.community.profile.dto;

import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.entity.User;

public record CommunityProfileInfo(
        Long id,
        String nickname,
        ImageInfo profileImage
) {
    public static CommunityProfileInfo from(User user) {
        return new CommunityProfileInfo(
                user.getId(),
                user.getNickname(),
                ImageInfo.from(user.getProfileImageFile())
        );
    }
}
