package com.jiujitsu.api.domain.competition.controller;

import com.jiujitsu.api.domain.competition.dto.CompetitionInfoReportRequest;
import com.jiujitsu.api.domain.competition.service.CompetitionInfoReportService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "competition-info-report-controller", description = "대회 정보 제보 API")
@CommonApiResponses
@RestController
@RequestMapping("/competitions/{competitionId}/info-reports")
@RequiredArgsConstructor
public class CompetitionInfoReportController {

    private final CompetitionInfoReportService competitionInfoReportService;

    @Operation(
            summary = "대회 정보 제보 (정보가 달라요)",
            description = "공개 대회의 정보가 대회 공지와 다를 때 제보합니다. 무엇이 다른지(field) 하나를 고르고, "
                    + "내용(detail, 최대 200자)은 OTHER 면 필수, 나머지는 선택입니다. 끝난 대회도 제보할 수 있습니다. "
                    + "같은 대회의 같은 항목은 하루(자정 기준) 3번까지 제보할 수 있습니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({
            ErrorCode.WRONG_PARAMETER,
            ErrorCode.COMPETITION_NOT_FOUND,
            ErrorCode.COMPETITION_INFO_REPORT_LIMIT_EXCEEDED
    })
    @PostMapping
    public void report(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId,
            @Valid @RequestBody CompetitionInfoReportRequest request
    ) {
        competitionInfoReportService.report(competitionId, request);
    }
}
