package com.jiujitsu.api.domain.community.board.dto;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

// 쿼리 파라미터 묶음 (컨트롤러에서 @ParameterObject). 필드 설명은 @Parameter, example 은 두지 않는다
@Schema(description = "결과 0건 검색어 조회 조건 (관리자)")
public record AdminSearchMissListRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Parameter(description = "이 날(포함)부터. 비우면 to 의 27일 전 (4주) (예: 2026-12-01)")
        LocalDate from,
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Parameter(description = "이 날(포함)까지. 비우면 오늘. from 보다 앞이면 400 (예: 2026-12-28)")
        LocalDate to,
        @Min(value = 1, message = "limit 은 1 이상이어야 합니다.")
        @Max(value = 500, message = "limit 은 500 이하여야 합니다.")
        @Parameter(description = "몇 개까지 (기본 100, 최대 500)")
        Integer limit
) { }
