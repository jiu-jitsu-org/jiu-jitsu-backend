package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionSourceSite;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "대회 출처 연결 요청")
public record CompetitionSourceRequest(
        @Schema(description = "출처 사이트")
        @NotNull
        CompetitionSourceSite site,

        @Schema(description = "원문 주소", example = "https://flowcomp.kr/competition/123")
        @NotBlank
        @Size(max = 1000)
        String sourceUrl,

        @Schema(description = "페이지 제목 (회차 구분에 사용)", nullable = true)
        @Size(max = 500)
        String pageTitle
) {}
