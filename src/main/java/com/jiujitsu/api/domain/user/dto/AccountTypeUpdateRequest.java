package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.AccountType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "계정 구분 변경 요청")
public record AccountTypeUpdateRequest(
        @Schema(description = "계정 구분 (MEMBER · OPERATOR · INTERNAL)", example = "INTERNAL")
        @NotNull
        AccountType accountType
) {
}
