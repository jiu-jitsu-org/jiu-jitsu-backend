package com.jiujitsu.api.domain.boot_strap.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "앱 공지 등록/수정 요청")
public record AdminAppNoticeRequest(
        @NotBlank @Size(max = 100)
        @Schema(description = "제목") String title,
        @NotBlank
        @Schema(description = "본문") String body,
        @Size(max = 500)
        @Schema(description = "링크 (없으면 null)") String link,
        @Schema(description = "앱 사용 차단 여부") boolean blockApp,
        @NotNull
        @Schema(description = "표시 시작 시각") LocalDateTime startAt,
        @NotNull
        @Schema(description = "표시 종료 시각") LocalDateTime endAt,
        @Schema(description = "활성화 여부") boolean active
) {}
