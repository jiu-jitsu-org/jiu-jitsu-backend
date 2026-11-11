package com.jiujitsu.api.domain.competition.controller;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionInfoReportResponse;
import com.jiujitsu.api.domain.competition.service.CompetitionInfoReportService;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-competition-info-report-controller", description = "관리자 대회 정보 제보 API")
@CommonApiResponses
@RestController
@RequestMapping("/admin/competitions/info-reports")
@RequiredArgsConstructor
public class AdminCompetitionInfoReportController {

    private final CompetitionInfoReportService competitionInfoReportService;

    @Operation(summary = "대회 정보 제보 목록", description = "대회 정보 제보를 최신순으로 페이징 조회합니다. 정렬은 고정이라 sort 는 무시합니다. (ROLE_ADMIN 필요)")
    @GetMapping
    public Page<AdminCompetitionInfoReportResponse> getReports(@PageableDefault(size = 20) Pageable pageable) {
        return competitionInfoReportService.getAdminReports(pageable);
    }
}
