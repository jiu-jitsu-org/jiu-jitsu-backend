package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionStatus;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

// 쿼리 파라미터 묶음 (컨트롤러에서 @ParameterObject). 필드 설명은 @Parameter — @Schema 를 붙이면 Swagger 가 타입을 string 으로 덮어쓴다
// example 은 size 만 둔다: Swagger "Try it out" 이 예시값을 미리 채워 date·keyword 등이 몰래 함께 전송된다
@Schema(description = "대회 목록 조회 조건")
public record CompetitionListRequest(
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        @Parameter(description = "기준일. 이 날(포함) 이후 대회부터 첫 페이지. 비우면 오늘. cursor 와 함께 보낼 수 없음 (예: 2026-11-09)")
        LocalDate date,
        @Parameter(description = "다음·이전 페이지 커서 (응답의 nextCursor·previousCursor 그대로)")
        String cursor,
        @Min(value = 1, message = "size 는 1 이상입니다.")
        @Max(value = 100, message = "size 는 100 이하입니다.")
        @Parameter(description = "페이지 크기 (1~100, 기본 20)", example = "20")
        Integer size,
        @Parameter(description = "지역(시·도). 여러 개면 그중 하나")
        List<CompetitionRegion> region,
        @Parameter(description = "오늘 기준 접수/진행 상태. 여러 개면 그중 하나")
        List<CompetitionStatus> status,
        @Parameter(description = "true 면 내 관심 대회만 (로그인 필요)")
        Boolean interested,
        @Parameter(description = "대회명 일부 (공백·대소문자 무시, 예: 서울)")
        String keyword
) {

    public static final int DEFAULT_SIZE = 20;

    public int sizeOrDefault() {
        return size == null ? DEFAULT_SIZE : size;
    }

    public boolean interestedOnly() {
        return Boolean.TRUE.equals(interested);
    }
}
