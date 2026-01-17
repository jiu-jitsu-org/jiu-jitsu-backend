package com.jiujitsu.api.domain.boot_strap.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "등록된 AppVersion 요청")
public record BootStrapRequest(
        @Schema(description = "OS 구분", example = "ANDROID / IOS")
        String osName
) {

    public BootStrapRequest(String osName) {
        String osValue = "ANDROID";
        if(osName != null) {
           osValue = osName;
        }

        this.osName = osValue;
    }
}
