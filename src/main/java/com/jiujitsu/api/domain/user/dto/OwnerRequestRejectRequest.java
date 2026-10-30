package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.OwnerVerificationRequest;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "관장/사범 인증 신청 거절 요청")
public record OwnerRequestRejectRequest(
        @Schema(description = "거절 사유 (선택, 신청자에게 노출)", example = "서류에서 지도자 정보를 확인할 수 없습니다.")
        @Size(max = OwnerVerificationRequest.REJECT_REASON_MAX_LENGTH)
        String reason
) {
}
