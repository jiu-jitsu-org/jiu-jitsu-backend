package com.jiujitsu.api.domain.user.factory;

import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.dto.SnsUserInfo;
import com.jiujitsu.api.domain.user.dto.UserInfo;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {
    public User createNewUser(SnsProvider snsProvider, SnsUserInfo snsUserInfo) {
        return User.builder()
                .email(snsUserInfo.getEmail())
                .nickname(snsUserInfo.getNickname())
                .snsProvider(snsProvider)
                .snsId(snsUserInfo.getSnsId())
                .ownerRequested(false)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    public UserInfo createUserInfo(User user, boolean deactivatedWithinGrace) {
        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                ImageInfo.from(user.getProfileImageFile()),
                user.getSnsProvider(),
                deactivatedWithinGrace
        );
    }
}
