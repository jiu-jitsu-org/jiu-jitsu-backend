package com.jiujitsu.api.domain.user.mapper;

import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.user.dto.OwnerVerificationInfo;
import com.jiujitsu.api.domain.user.entity.OwnerVerificationRequest;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = new UserMapper();
    private final User user = User.builder().id(7L).role(UserRole.USER).build();

    private OwnerVerificationRequest pending() {
        return OwnerVerificationRequest.builder()
                .user(user)
                .documentImageFile(ImageFile.builder().id(10L).imageUrl("https://cdn/10").build())
                .build();
    }

    @Test
    @DisplayName("신청이 없으면 ownerVerification.status 는 NONE 이고 나머지는 null")
    void none() {
        OwnerVerificationInfo info = userMapper.toUserProfileResponse(user, null).ownerVerification();

        assertThat(info.status()).isEqualTo("NONE");
        assertThat(info.documentImage()).isNull();
        assertThat(info.rejectReason()).isNull();
        assertThat(info.requestedAt()).isNull();
    }

    @Test
    @DisplayName("심사 대기면 PENDING 과 제출 서류를 내려준다")
    void pendingRequest() {
        OwnerVerificationInfo info = userMapper.toUserProfileResponse(user, pending()).ownerVerification();

        assertThat(info.status()).isEqualTo("PENDING");
        assertThat(info.documentImage().imageUrl()).isEqualTo("https://cdn/10");
    }

    @Test
    @DisplayName("거절이면 REJECTED 와 사유를 내려준다")
    void rejectedRequest() {
        OwnerVerificationRequest request = pending();
        request.reject(User.builder().id(1L).role(UserRole.ADMIN).build(), "서류 불명확");

        OwnerVerificationInfo info = userMapper.toUserProfileResponse(user, request).ownerVerification();

        assertThat(info.status()).isEqualTo("REJECTED");
        assertThat(info.rejectReason()).isEqualTo("서류 불명확");
    }
}
