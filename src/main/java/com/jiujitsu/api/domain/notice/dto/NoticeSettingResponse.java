package com.jiujitsu.api.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 수신동의여부 응답")
public record NoticeSettingResponse(
        @Schema(description = "계정보안알림") Boolean securityEnabled,
        @Schema(description = "서비스공지알림") Boolean serviceEnabled,
        @Schema(description = "커뮤니티활동알림") Boolean communityEnabled,
        @Schema(description = "마케팅정보알림") Boolean marketingEnabled,
        @Schema(description = "마케팅 수신 동의 일시 (미동의 시 null)") LocalDateTime marketingAgreedAt
) {
}
