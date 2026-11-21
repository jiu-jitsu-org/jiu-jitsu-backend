package com.jiujitsu.api.domain.announcement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공지 목록 항목")
public record AnnouncementListResponse(
        @Schema(description = "공지 ID", example = "1")
        Long announcementId,
        @Schema(description = "제목", example = "서비스 이용약관 변경 안내")
        String title,
        @Schema(description = "게시 시각 (화면에 보이는 날짜)", example = "2026-12-01T10:00:00")
        LocalDateTime publishedAt
) {
}
