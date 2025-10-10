package com.jiujitsu.api.domain.boot_strap.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "앱 버전 응답")
public record AppVersionResponse(
        @Schema(description = "최소 버전 정보 (항상 강제 업데이트 진행)")
        String minVersion,
        @Schema(description = "현재 정보 (최신 버전 정보)")
        String nowVersion,
        @Schema(description = "강제 업데이트 사용 유무 (현재 버전을 비교하여 선택/강제 업데이트)")
        boolean needForceUpdate
) { }
