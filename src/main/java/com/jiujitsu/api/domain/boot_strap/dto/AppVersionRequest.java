package com.jiujitsu.api.domain.boot_strap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Schema(description = "등록된 AppVersion 요청")
public record AppVersionRequest(
        @Schema(description = "OS 구분", example = "ANDROID / IOS")
        String osName
) {

    public AppVersionRequest(String osName) {
        String osValue = "ANDROID";
        if(osName != null) {
           osValue = osName;
        }

        this.osName = osValue;
    }
}
