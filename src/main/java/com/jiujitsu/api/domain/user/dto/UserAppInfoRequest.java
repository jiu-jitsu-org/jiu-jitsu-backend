package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.boot_strap.entity.OsType;
import com.jiujitsu.api.domain.user.entity.UserAppInfo;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "사용자 앱 정보")
public class UserAppInfoRequest {
    @NotBlank(message = "fcm 토큰은 필수입니다.")
    @Schema(description = "fcm 푸시 전송용 토큰", example = "fcmfcm239efwfcm")
    private String fcmToken;

    @NotBlank(message = "device id값은 필수입니다.")
    @Schema(description = "디바이스 id(앱 생성)", example = "android3294f2i4dwe")
    private String deviceId;

    @NotBlank(message = "OS 타입은 필수입니다.")
    @Schema(description = "OS Type")
    private OsType osType;

    @NotBlank(message = "OS 버전은 필수입니다.")
    @Schema(description = "OS Version", example = "18.0.2")
    private String osVersion;

    public UserAppInfo toEntity() {
        return UserAppInfo.builder()
                .token(this.fcmToken)
                .deviceId(this.deviceId)
                .osType(this.osType)
                .osVersion(this.osVersion)
                .build();
    }
}
