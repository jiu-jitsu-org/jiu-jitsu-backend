package com.jiujitsu.api.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "관리자 로그인 요청")
public record AdminLoginRequest(
        @NotBlank
        @Schema(description = "관리자 이메일") String email,
        @NotBlank
        @Schema(description = "관리자 비밀번호") String password
) {
}
