package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.entity.OwnerVerificationRequest;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관장/사범 인증 신청 상태 (최신 신청 기준)")
public record OwnerVerificationInfo(
        @Schema(description = "신청 상태 (NONE: 신청 없음)", example = "PENDING", allowableValues = {"NONE", "PENDING", "APPROVED", "REJECTED"}) String status,
        @Schema(description = "제출 서류 이미지") ImageInfo documentImage,
        @Schema(description = "거절 사유 (REJECTED 일 때만)") String rejectReason,
        @Schema(description = "신청 일시") LocalDateTime requestedAt
) {
    private static final OwnerVerificationInfo NONE = new OwnerVerificationInfo("NONE", null, null, null);

    public static OwnerVerificationInfo from(OwnerVerificationRequest request) {
        if (request == null) return NONE;
        return new OwnerVerificationInfo(
                request.getStatus().name(),
                ImageInfo.from(request.getDocumentImageFile()),
                request.getRejectReason(),
                request.getCreatedAt()
        );
    }
}
