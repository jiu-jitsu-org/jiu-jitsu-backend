package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.SnsProvider;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "SNS 로그인 요청")
public class SnsLoginRequest {

    @NotNull(message = "SNS 제공자는 필수입니다")
    @Schema(description = "SNS 제공자", example = "KAKAO")
    private SnsProvider snsProvider;

    @NotBlank(message = "액세스 토큰은 필수입니다")
    @Schema(description = "SNS에서 발급받은 액세스 토큰", example = "ya29.a0AfH6SMC...")
    private String accessToken;

    public SnsLoginRequest(SnsProvider snsProvider, String accessToken, String idToken) {
        this.snsProvider = snsProvider;
        this.accessToken = accessToken;
    }
}
