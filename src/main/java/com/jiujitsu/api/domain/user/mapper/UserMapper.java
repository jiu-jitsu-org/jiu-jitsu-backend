package com.jiujitsu.api.domain.user.mapper;

import com.jiujitsu.api.domain.user.dto.UserProfileResponse;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    /**
     * user profile
     */
    public UserProfileResponse toUserProfileResponse(User user) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getSnsProvider(),
                user.getOwnerRequested(),
                user.getOwnerRequestImageUrl(),
                user.getRole(),
                user.getStatus()
        );
    }
}
