package com.jiujitsu.api.domain.boot_strap.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "관리자 앱 버전 응답")
public record AdminAppVersionResponse(
        @Schema(description = "OS 종류", example = "IOS")
        String osName,

        @Schema(description = "최소 허용 버전", example = "1.2.0")
        String minVersion,

        @Schema(description = "최신(권장) 버전", example = "1.3.0")
        String nowVersion,

        @Schema(description = "마지막 수정 시각")
        Instant updatedAt
) {}
