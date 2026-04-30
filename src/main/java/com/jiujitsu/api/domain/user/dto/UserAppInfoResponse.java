package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.boot_strap.entity.OsType;
import com.jiujitsu.api.domain.user.entity.UserAppInfo;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "app info response")
public record UserAppInfoResponse(
        String token,
        String deviceId,
        OsType osType,
        String osVersion,
        String name
) {
    public static UserAppInfoResponse toResponse(UserAppInfo userAppInfo, String name) {
        return new UserAppInfoResponse(
                userAppInfo.getToken(),
                userAppInfo.getDeviceId(),
                userAppInfo.getOsType(),
                userAppInfo.getOsVersion(),
                name
        );
    }
}
