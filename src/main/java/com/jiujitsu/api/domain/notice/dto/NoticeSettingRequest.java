package com.jiujitsu.api.domain.notice.dto;

import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "알림 수신동의여부 요청")
public record NoticeSettingRequest(
        @Schema(description = "계정보안알림") Boolean securityEnabled,
        @Schema(description = "서비스공지알림") Boolean serviceEnabled,
        @Schema(description = "커뮤니티활동알림") Boolean communityEnabled,
        @Schema(description = "마케팅정보알림") Boolean marketingEnabled,
        @Schema(description = "관심 대회 알림 (얼리버드·접수 마감 전날). 보내지 않으면 그대로", nullable = true) Boolean competitionEnabled,
        @Schema(description = "밸런스 게임 결과 알림. 보내지 않으면 그대로", nullable = true) Boolean balanceResultEnabled
) {
    public UserNoticeSetting toEntity() {
        return UserNoticeSetting.builder()
                .securityEnabled(this.securityEnabled)
                .serviceEnabled(this.serviceEnabled)
                .communityEnabled(this.communityEnabled)
                .marketingEnabled(this.marketingEnabled)
                .competitionEnabled(this.competitionEnabled)
                .balanceResultEnabled(this.balanceResultEnabled)
                .build();
    }
}
