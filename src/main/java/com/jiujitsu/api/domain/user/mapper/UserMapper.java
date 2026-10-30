package com.jiujitsu.api.domain.user.mapper;

import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.dto.OwnerVerificationInfo;
import com.jiujitsu.api.domain.user.dto.UserProfileResponse;
import com.jiujitsu.api.domain.user.entity.OwnerVerificationRequest;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {
    /**
     * @param latestOwnerRequest 유저의 최신 관장/사범 인증 신청 (없으면 null)
     */
    public UserProfileResponse toUserProfileResponse(User user, OwnerVerificationRequest latestOwnerRequest) {
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                user.getNickname(),
                ImageInfo.from(user.getProfileImageFile()),
                user.getSnsProvider(),
                OwnerVerificationInfo.from(latestOwnerRequest),
                user.getRole(),
                user.getStatus()
        );
    }
}
