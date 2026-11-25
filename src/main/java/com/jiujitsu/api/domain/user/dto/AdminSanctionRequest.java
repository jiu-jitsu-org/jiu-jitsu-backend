package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.SanctionReason;
import com.jiujitsu.api.domain.user.entity.SanctionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AdminSanctionRequest(
        @NotNull
        SanctionType type,

        @NotNull
        SanctionReason reason,

        @Schema(description = "기타 사유 상세 (OTHER 선택 시 필수, 최대 500자)")
        @Size(max = 500)
        String reasonDetail,

        @Schema(description = "일시정지 기간(일). SUSPENSION 시 필수")
        Integer durationDays
) {}
