package com.jiujitsu.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

public record DeactivateRequest(
        @Schema(description = "Apple 전용: Sign in with Apple 시 발급된 authorization_code. Kakao/Google은 생략 가능.")
        String authorizationCode
) {
    public DeactivateRequest() {
        this(null);
    }
}
