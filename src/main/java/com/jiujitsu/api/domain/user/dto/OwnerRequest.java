package com.jiujitsu.api.domain.user.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관장/사범 권한 요청")
public record OwnerRequest(
        @Schema(description = "서류 이미지 URL", example = "https://example.com/profile.jpg")
        String documentImageUrl
) {
}
