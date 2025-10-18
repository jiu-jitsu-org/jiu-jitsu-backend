package com.jiujitsu.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "회원가입 요청")
public class CreateProfileRequest {

    @NotBlank(message = "닉네임은 필수입니다.")
    @Size(min = 2, max = 20, message = "닉네임은 2자 이상 20자 이하여야 합니다")
    @Schema(description = "사용자 닉네임", example = "홍길동")
    private String nickname;

    @Schema(description = "선택약관 동의여부")
    private Boolean isMarketingAgreed;
}
