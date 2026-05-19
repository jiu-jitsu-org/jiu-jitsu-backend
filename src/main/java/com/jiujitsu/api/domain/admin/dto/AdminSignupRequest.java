package com.jiujitsu.api.domain.admin.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "관리자 회원가입 요청")
public record AdminSignupRequest(
        @NotBlank(message = "이메일을 입력해 주세요.")
        @Email(message = "이메일 형식으로 입력해 주세요.")
        @Schema(description = "관리자 이메일") String email,
        @NotBlank(message = "비밀번호를 입력해 주세요.")
        @Size(min = 8, message = "비밀번호는 8자 이상으로 입력해 주세요.")
        @Schema(description = "관리자 비밀번호 (8자 이상)") String password
) {
}
