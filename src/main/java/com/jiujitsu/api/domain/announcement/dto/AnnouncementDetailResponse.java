package com.jiujitsu.api.domain.announcement.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "공지 상세")
public record AnnouncementDetailResponse(
        @Schema(description = "공지 ID", example = "1")
        Long announcementId,
        @Schema(description = "제목", example = "서비스 이용약관 변경 안내")
        String title,
        @Schema(description = "본문 (평문, 줄바꿈 \\n 그대로. 링크는 화면에서 주소를 찾아 링크로)", example = "12월 8일부터 이용약관이 바뀝니다.\n자세한 내용은 https://example.com 에서 볼 수 있어요.")
        String body,
        @Schema(description = "게시 시각 (화면에 보이는 날짜)", example = "2026-12-01T10:00:00")
        LocalDateTime publishedAt
) {
}
