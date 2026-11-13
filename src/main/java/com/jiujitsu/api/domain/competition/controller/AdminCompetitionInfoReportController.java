package com.jiujitsu.api.domain.competition.controller;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionInfoReportListRequest;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionInfoReportResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionInfoReportResolveRequest;
import com.jiujitsu.api.domain.competition.service.CompetitionInfoReportService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-competition-info-report-controller", description = "관리자 대회 정보 제보 API")
@CommonApiResponses
@RestController
@RequestMapping("/admin/competitions/info-reports")
@RequiredArgsConstructor
public class AdminCompetitionInfoReportController {

    private final CompetitionInfoReportService competitionInfoReportService;

    @Operation(summary = "대회 정보 제보 목록",
            description = "대회 정보 제보를 미처리 먼저 → 최신순으로 페이징 조회합니다. resolved(처리 여부) · competitionId(대회별)로 거를 수 있습니다. 정렬은 고정이라 sort 는 무시합니다. "
                    + "페이지는 순서 기준이라 처리하면 미처리 제보가 앞으로 당겨지므로, 처리한 뒤에는 첫 페이지부터 다시 조회합니다. (ROLE_ADMIN 필요)")
    @ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER})
    @GetMapping
    public Page<AdminCompetitionInfoReportResponse> getReports(@ParameterObject AdminCompetitionInfoReportListRequest request,
                                                               @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return competitionInfoReportService.getAdminReports(request, pageable);
    }

    @Operation(summary = "대회 정보 제보 처리 완료",
            description = "제보를 처리한 것으로 표시합니다. 메모(note, 선택, 100자)로 무엇을 했는지 남깁니다. 다시 보내면 처음 처리 시각은 그대로 둡니다. "
                    + "메모는 note 를 보냈을 때만 바꾸고(빼거나 본문 없이 보내면 그대로), 빈 문자열이면 지웁니다. 제보자에게 알리지 않습니다. (ROLE_ADMIN 필요)")
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_INFO_REPORT_NOT_FOUND, ErrorCode.WRONG_PARAMETER})
    @PatchMapping("/{reportId}/resolve")
    public AdminCompetitionInfoReportResponse resolve(
            @Parameter(description = "제보 ID", required = true) @PathVariable Long reportId,
            @Valid @RequestBody(required = false) CompetitionInfoReportResolveRequest request) {
        return competitionInfoReportService.resolve(reportId, request);
    }

    @Operation(summary = "대회 정보 제보 처리 되돌리기", description = "처리 시각 · 메모를 지워 미처리로 돌립니다. (ROLE_ADMIN 필요)")
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_INFO_REPORT_NOT_FOUND})
    @PatchMapping("/{reportId}/unresolve")
    public AdminCompetitionInfoReportResponse unresolve(@Parameter(description = "제보 ID", required = true) @PathVariable Long reportId) {
        return competitionInfoReportService.unresolve(reportId);
    }
}
