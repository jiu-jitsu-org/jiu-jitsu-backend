package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "대회가 있는 지역 (대회 탭 지역 필터)")
public record CompetitionRegionsResponse(
        @Schema(description = "공개 대회(지난 대회 포함)가 있는 지역. 지역 enum 선언 순서, 중복 없음")
            List<CompetitionRegion> regions
) { }
