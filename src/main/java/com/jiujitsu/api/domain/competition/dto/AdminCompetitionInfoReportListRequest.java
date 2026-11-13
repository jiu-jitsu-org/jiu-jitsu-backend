package com.jiujitsu.api.domain.competition.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;

// 쿼리 파라미터 묶음 (컨트롤러에서 @ParameterObject). 필드 설명은 @Parameter, example 은 두지 않는다 (CompetitionListRequest 참고)
@Schema(description = "관리자 대회 정보 제보 목록 조건")
public record AdminCompetitionInfoReportListRequest(
        @Parameter(description = "처리 여부. 비우면 전체 (미처리 먼저)")
        Boolean resolved,
        @Parameter(description = "대회 ID — 그 대회 제보만")
        Long competitionId
) { }
