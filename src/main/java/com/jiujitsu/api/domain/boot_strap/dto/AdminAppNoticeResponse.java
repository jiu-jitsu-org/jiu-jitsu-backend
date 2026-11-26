package com.jiujitsu.api.domain.boot_strap.dto;

import com.jiujitsu.api.domain.boot_strap.entity.AppNotice;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "앱 공지 관리자 응답")
public record AdminAppNoticeResponse(
        Long id,
        String title,
        String body,
        String link,
        boolean blockApp,
        LocalDateTime startAt,
        LocalDateTime endAt,
        boolean active,
        LocalDateTime updatedAt
) {
    public static AdminAppNoticeResponse from(AppNotice notice) {
        return new AdminAppNoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.getLink(),
                notice.isBlockApp(),
                notice.getStartAt(),
                notice.getEndAt(),
                notice.isActive(),
                notice.getUpdatedAt()
        );
    }
}
