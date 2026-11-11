package com.jiujitsu.api.domain.notice.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "처음 관심 등록 때 관심 대회 알림 질문의 답")
public record CompetitionNoticePromptRequest(
        @NotNull(message = "enabled 는 필수입니다.")
        @Schema(description = "알림 받기 = true, 나중에 = false", requiredMode = Schema.RequiredMode.REQUIRED)
        Boolean enabled
) {
}
