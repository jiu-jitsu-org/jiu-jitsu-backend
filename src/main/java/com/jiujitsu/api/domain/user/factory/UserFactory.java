package com.jiujitsu.api.domain.user.factory;

import com.jiujitsu.api.domain.user.dto.SnsUserInfo;
import com.jiujitsu.api.domain.user.dto.UserInfo;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import org.springframework.stereotype.Component;

@Component
public class UserFactory {
    /**
     * user entity 생성
     */
    public User createNewUser(SnsProvider snsProvider, SnsUserInfo snsUserInfo) {
        return User.builder()
                .email(snsUserInfo.getEmail())
                .nickname(snsUserInfo.getNickname())
                .profileImageUrl("default")
                .snsProvider(snsProvider)
                .snsId(snsUserInfo.getSnsId())
                .ownerRequested(false)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();
    }

    /**
     * userInfo 생성
     */
    public UserInfo createUserInfo(User user, boolean deactivatedWithinGrace) {
        return new UserInfo(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                user.getProfileImageUrl(),
                user.getSnsProvider(),
                deactivatedWithinGrace
        );
    }

}
