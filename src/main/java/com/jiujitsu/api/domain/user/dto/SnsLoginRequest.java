package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.SnsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "SNS 로그인 요청")
public record SnsLoginRequest(
        @NotNull(message = "SNS 제공자는 필수입니다")
        @Schema(description = "SNS 제공자", example = "KAKAO")
                SnsProvider snsProvider,
        @NotBlank(message = "액세스 토큰은 필수입니다")
        @Schema(description = "SNS에서 발급받은 액세스 토큰", example = "ya29a0AfH6SMC")
                String accessToken
) {
}
