package com.jiujitsu.api.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "관리자 토큰 갱신 요청")
public record AdminRefreshRequest(
        @NotBlank
        @Schema(description = "리프레시 토큰") String refreshToken
) {
}
