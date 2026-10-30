package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.file.dto.ImageInfo;
import com.jiujitsu.api.domain.user.entity.OwnerVerificationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "관리자 관장/사범 인증 신청 목록 항목")
public record AdminOwnerRequestResponse(
        @Schema(description = "신청 ID", example = "1") Long requestId,
        @Schema(description = "신청자 ID", example = "7") Long userId,
        @Schema(description = "신청자 닉네임") String nickname,
        @Schema(description = "신청자 이메일") String email,
        @Schema(description = "신청자 프로필 이미지") ImageInfo profileImage,
        @Schema(description = "제출 서류 이미지") ImageInfo documentImage,
        @Schema(description = "커뮤니티 프로필 보유 여부 (없으면 승인 불가)") boolean hasCommunityProfile,
        @Schema(description = "신청 상태", example = "PENDING") OwnerVerificationStatus status,
        @Schema(description = "거절 사유") String rejectReason,
        @Schema(description = "신청 일시") LocalDateTime requestedAt,
        @Schema(description = "처리 일시") LocalDateTime reviewedAt
) {
}
