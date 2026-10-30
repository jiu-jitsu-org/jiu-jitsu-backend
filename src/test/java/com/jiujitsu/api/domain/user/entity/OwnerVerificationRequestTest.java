package com.jiujitsu.api.domain.user.entity;

import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnerVerificationRequestTest {

    private final User admin = User.builder().id(1L).role(UserRole.ADMIN).build();

    private OwnerVerificationRequest pending() {
        return OwnerVerificationRequest.builder()
                .user(User.builder().id(2L).role(UserRole.USER).build())
                .documentImageFile(ImageFile.builder().id(10L).imageUrl("a").build())
                .build();
    }

    @Test
    @DisplayName("새 신청은 PENDING 이다")
    void defaultPending() {
        assertThat(pending().getStatus()).isEqualTo(OwnerVerificationStatus.PENDING);
    }

    @Test
    @DisplayName("승인하면 APPROVED 와 처리자·처리 시각이 기록된다")
    void approve() {
        OwnerVerificationRequest request = pending();

        request.approve(admin);

        assertThat(request.getStatus()).isEqualTo(OwnerVerificationStatus.APPROVED);
        assertThat(request.getReviewedBy()).isSameAs(admin);
        assertThat(request.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("거절하면 REJECTED 와 사유가 기록된다")
    void reject() {
        OwnerVerificationRequest request = pending();

        request.reject(admin, "서류 불명확");

        assertThat(request.getStatus()).isEqualTo(OwnerVerificationStatus.REJECTED);
        assertThat(request.getRejectReason()).isEqualTo("서류 불명확");
        assertThat(request.getReviewedBy()).isSameAs(admin);
        assertThat(request.getReviewedAt()).isNotNull();
    }

    @Test
    @DisplayName("처리된 신청은 다시 승인·거절·서류 교체할 수 없다")
    void notPending() {
        OwnerVerificationRequest request = pending();
        request.reject(admin, null);

        assertThatThrownBy(() -> request.approve(admin))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.OWNER_REQUEST_NOT_PENDING);
        assertThatThrownBy(() -> request.reject(admin, null))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.OWNER_REQUEST_NOT_PENDING);
        assertThatThrownBy(() -> request.replaceDocument(ImageFile.builder().id(11L).imageUrl("b").build()))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.OWNER_REQUEST_NOT_PENDING);
    }
}
