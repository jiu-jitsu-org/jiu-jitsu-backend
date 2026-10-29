package com.jiujitsu.api.domain.community.profile.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "메달 개수 합계 (부문 기준, 참가 제외)")
public record MedalCountDto(
        @Schema(description = "금메달", example = "2") int gold,
        @Schema(description = "은메달", example = "1") int silver,
        @Schema(description = "동메달", example = "0") int bronze
) {
}
