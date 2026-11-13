package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionAudience;
import com.jiujitsu.api.domain.competition.entity.CompetitionEventInfo;
import com.jiujitsu.api.domain.competition.entity.CompetitionFee;
import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionUniform;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;

// 만들기·고치기 공통. 고치기는 전체 교체 — 안 보낸 칸·null 은 비운다
// 글자 칸 길이는 컬럼 길이와 같다 (넘으면 저장 시점 500 이 되므로 여기서 400)
@Schema(description = "관리자 대회 정보 입력 (전체 칸. 안 보낸 칸·null 은 비움)")
public record AdminCompetitionRequest(
        @NotBlank
        @Size(max = 100)
        @Schema(description = "대회명", example = "2026 서울 오픈 주짓수 챔피언십", requiredMode = Schema.RequiredMode.REQUIRED)
            String name,
        @NotNull
        @Min(1990L)
        @Max(2999L)
        @Schema(description = "개최 연도. 대회일 연도와 같아야 함", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
            Integer competitionYear,
        @NotNull
        @Schema(description = "대회일 (여러 날 대회면 첫날)", example = "2026-12-06", requiredMode = Schema.RequiredMode.REQUIRED)
            LocalDate openDate,
        @Schema(description = "여러 날 대회의 마지막 날. 하루짜리면 비움(대회일과 같으면 비운 것으로 저장). 대회일보다 앞이거나 14일 넘게 뒤면 400. 고치기(PUT)에서 빼면 비워짐", example = "2026-12-07", nullable = true)
            LocalDate endDate,
        @Schema(description = "시작 시각 HH:mm (초를 붙여 보내면 분 단위로 잘라 저장)", type = "string", example = "09:00", nullable = true)
            LocalTime startTime,
        @Schema(description = "지역(시·도)", nullable = true)
            CompetitionRegion region,
        @Size(max = 200)
        @Schema(description = "짧은 장소 표기 (목록에 씀)", example = "서울 송파", nullable = true)
            String place,
        @Size(max = 100)
        @Schema(description = "경기장 이름", example = "올림픽홀", nullable = true)
            String venue,
        @Size(max = 100)
        @Schema(description = "주최 단체", example = "서울시주짓수회", nullable = true)
            String host,
        @Size(max = 500)
        @Pattern(regexp = "^\\s*https?://.*", message = "대회 공지 링크는 http:// 또는 https:// 로 시작해야 합니다.")
        @Schema(description = "대회 공지 링크 (http·https)", example = "https://example.com/notice", nullable = true)
            String noticeUrl,
        @Schema(description = "종목 (도복/노기). 비우면 모름", nullable = true)
            List<@NotNull CompetitionUniform> uniforms,
        @Schema(description = "참가 대상 제한 (제한 없으면 null)", nullable = true)
            CompetitionAudience audience,
        @Schema(description = "접수 시작일", nullable = true)
            LocalDate registrationStartDate,
        @Schema(description = "얼리버드 마감일. 기한 있는 참가비 단계가 있으면 마지막 단계 기한과 같아야 함", nullable = true)
            LocalDate earlyBirdDeadline,
        @Schema(description = "접수 마감일", nullable = true)
            LocalDate registrationDeadline,
        @Size(max = 10)
        @Schema(description = "참가비 단계 (표시 순서). 기한은 오름차순, 기한 없는 단계(기본 참가비)는 맨 끝 하나만. 비우면 모름", nullable = true)
            List<@Valid @NotNull Fee> fees,
        @Size(max = 100)
        @Schema(description = "참가비 기준 문구", example = "성인 1종목 기준", nullable = true)
            String feeNote
) {

    /**
     * 대회 정보 칸으로 (Competition#changeEventInfo). 글자 칸은 앞뒤 공백을 지우고 비면 null — 빈 문자열이 "채워진 칸"으로 남지 않게
     * 종목·참가비는 null 이면 빈 컬렉션, 엔티티가 옮겨 담으므로 가변으로 만든다
     */
    public CompetitionEventInfo toEventInfo() {
        return new CompetitionEventInfo(
                openDate,
                endDate,
                startTime == null ? null : startTime.truncatedTo(ChronoUnit.MINUTES),   // 응답이 HH:mm 라 초를 남기면 보이지 않는 값이 생긴다
                region,
                blankToNull(place),
                blankToNull(venue),
                blankToNull(host),
                blankToNull(noticeUrl),
                uniforms == null ? new LinkedHashSet<>() : new LinkedHashSet<>(uniforms),
                audience,
                registrationStartDate,
                earlyBirdDeadline,
                registrationDeadline,
                fees == null ? new ArrayList<>() : fees.stream()
                        .map(fee -> new CompetitionFee(fee.label().strip(), fee.amount(), fee.until()))
                        .collect(Collectors.toCollection(ArrayList::new)),
                blankToNull(feeNote)
        );
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String stripped = value.strip();
        return stripped.isEmpty() ? null : stripped;
    }

    @Schema(description = "참가비 한 단계")
    public record Fee(
            @NotBlank
            @Size(max = CompetitionFee.LABEL_MAX_LENGTH)
            @Schema(description = "단계 이름", example = "얼리버드", requiredMode = Schema.RequiredMode.REQUIRED)
                String label,
            @NotNull
            @Min(0L)
            @Schema(description = "금액 (원, 0 = 무료)", example = "60000", requiredMode = Schema.RequiredMode.REQUIRED)
                Integer amount,
            @Schema(description = "이 금액의 마지막 날(당일 포함). null 이면 기한 없음(기본 참가비)", example = "2026-11-20", nullable = true)
                LocalDate until
    ) { }
}
