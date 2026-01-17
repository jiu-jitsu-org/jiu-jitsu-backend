package com.jiujitsu.api.domain.boot_strap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Objects;

@Schema(description = "BootStrap 응답")
public record BootStrapResponse(
        @Schema(description = "앱 버전 정보")
        AppVersionResponse appVersionInfo,
        @Schema(description = "Test Data")
        String test
) {
        public BootStrapResponse {
                Objects.requireNonNull(
                        appVersionInfo,"appVersionInfo must not be null"
                );

                Objects.requireNonNull(
                        test, "test data must not be null"
                );
        }
}
