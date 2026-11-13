package com.jiujitsu.api.domain.competition.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// 쿼리 파라미터 묶음 (컨트롤러에서 @ParameterObject). 필드 설명은 @Parameter, example 은 두지 않는다 (CompetitionListRequest 참고)
@Schema(description = "관리자 대회 목록 조회 조건")
public record AdminCompetitionListRequest(
        @Parameter(description = "공개 여부. 비우면 공개·미공개 모두")
        Boolean published,
        @Parameter(description = "대회명 일부 (공백·대소문자 무시, 예: 서울)")
        String keyword,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Parameter(description = "대회일 이 날(포함) 이후 (예: 2026-11-01)")
        LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Parameter(description = "대회일 이 날(포함) 이전. from 보다 앞이면 400 (예: 2026-12-31)")
        LocalDate to
) { }
