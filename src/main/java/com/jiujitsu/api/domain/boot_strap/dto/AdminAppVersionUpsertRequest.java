package com.jiujitsu.api.domain.boot_strap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

@Schema(description = "앱 버전 등록·수정 요청")
public record AdminAppVersionUpsertRequest(
        @Schema(description = "최소 허용 버전 (숫자와 점으로 구성, 예: 1.2.0)", example = "1.2.0")
        @NotBlank
        @Pattern(regexp = "\\d+(\\.\\d+)+", message = "버전은 숫자와 점(.)으로 구성해야 합니다. 예: 1.2.0")
        String minVersion,

        @Schema(description = "최신(권장) 버전 (숫자와 점으로 구성, 예: 1.3.0)", example = "1.3.0")
        @NotBlank
        @Pattern(regexp = "\\d+(\\.\\d+)+", message = "버전은 숫자와 점(.)으로 구성해야 합니다. 예: 1.3.0")
        String nowVersion
) {}
