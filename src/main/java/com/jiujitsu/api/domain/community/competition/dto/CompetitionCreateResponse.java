package com.jiujitsu.api.domain.community.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "등록 대회 일괄 등록 결과 (관리자)")
public record CompetitionCreateResponse(
        @Schema(description = "새로 등록된 대회")
            List<CompetitionResponse> created,
        @Schema(description = "건너뛴 대회와 사유")
            List<Skipped> skipped
) {
    @Schema(description = "건너뛴 대회")
    public record Skipped(
            @Schema(description = "대회명 (요청 원문, 중복은 정리한 이름)", example = "서울 오픈")
                String name,
            @Schema(description = "개최 연도", example = "2026", nullable = true)
                Integer year,
            @Schema(description = "건너뛴 사유", example = "DUPLICATE")
                SkipReason reason
    ) { }

    @Schema(description = "DUPLICATE: 이미 등록됐거나 요청 안에서 겹침(이름 공백·대소문자 무시 + 연도 같음), "
            + "INVALID_NAME: 공백·보이지 않는 문자를 정리하면 비거나 100자를 넘음")
    public enum SkipReason {
        DUPLICATE,
        INVALID_NAME
    }
}
