package com.jiujitsu.api.domain.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "관리자 대회 만들기 결과")
public record AdminCompetitionSaveResponse(
        @Schema(description = "true 면 같은 이름·연도의 자동완성용 대회에 정보를 채움 (새 대회를 만들지 않고 그 대회 ID 그대로, 프로필 대회 기록 연결 유지)")
            boolean catalogFilled,
        @Schema(description = "저장된 대회")
            AdminCompetitionDetailResponse competition
) { }
