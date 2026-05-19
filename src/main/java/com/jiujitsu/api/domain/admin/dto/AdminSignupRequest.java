package com.jiujitsu.api.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 회원가입 요청")
public record AdminSignupRequest(
        @NotBlank @Email
        @Schema(description = "관리자 이메일") String email,
        @NotBlank @Size(min = 8)
        @Schema(description = "관리자 비밀번호 (8자 이상)") String password
) {
}
