package com.jiujitsu.api.domain.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "CDN 서명 반환 응답")
public record CdnSignatureResponse(
        @Schema(description = "token")
        String token,
        @Schema(description = "만료시간")
        Long expire,
        @Schema(description = "서명")
        String signature
) {
}
