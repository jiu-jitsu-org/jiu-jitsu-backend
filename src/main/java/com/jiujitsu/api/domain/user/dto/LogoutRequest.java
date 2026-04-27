package com.jiujitsu.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "로그아웃 요청")
public record LogoutRequest(
        @NotBlank(message = "액세스 토큰은 필수입니다")
        @Schema(description = "로그아웃할 액세스 토큰", example = "eyJhbGciOiJIUzI1NiIs...")
            String accessToken,
        @Schema(description = "리프레시 토큰 (선택사항)", example = "eyJhbGciOiJIUzI1NiIs...")
            String refreshToken
) {
}
