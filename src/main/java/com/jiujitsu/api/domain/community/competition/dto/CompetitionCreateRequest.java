package com.jiujitsu.api.domain.community.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.util.List;

@Schema(description = "등록 대회 일괄 등록 요청 (관리자)")
public record CompetitionCreateRequest(
        @NotEmpty
        @Size(max = 200)
        @Schema(description = "등록할 대회 목록 (최대 200개)", requiredMode = Schema.RequiredMode.REQUIRED)
            List<@Valid @NotNull Item> competitions
) {
    @Schema(description = "등록할 대회")
    public record Item(
            @NotBlank
            @Size(max = 100)
            @Schema(description = "대회명", example = "2026 서울 오픈 주짓수 챔피언십", requiredMode = Schema.RequiredMode.REQUIRED)
                String name,
            @Min(1990L)
            @Max(2999L)
            @Schema(description = "개최 연도 (선택)", example = "2026")
                Integer year
    ) { }
}
