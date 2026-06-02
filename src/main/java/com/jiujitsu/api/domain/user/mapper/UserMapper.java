package com.jiujitsu.api.domain.user.mapper;

import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.dto.UserProfileResponse;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    public UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                ImageInfo.from(user.getProfileImageFile()),
                user.getSnsProvider(),
                user.getOwnerRequested(),
                ImageInfo.from(user.getOwnerRequestImageFile()),
                user.getRole(),
                user.getStatus()
        );
    }
}
