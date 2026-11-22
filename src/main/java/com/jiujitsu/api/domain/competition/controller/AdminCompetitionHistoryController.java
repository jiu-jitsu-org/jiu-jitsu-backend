package com.jiujitsu.api.domain.competition.controller;

import com.jiujitsu.api.domain.competition.dto.ChangeHistoryResponse;
import com.jiujitsu.api.domain.competition.service.AdminCompetitionHistoryService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-competition-history-controller", description = "관리자 대회 변경 이력 API")
@CommonApiResponses
@AdminOnly
@RestController
@RequestMapping("/admin/competitions")
@RequiredArgsConstructor
public class AdminCompetitionHistoryController {

    private final AdminCompetitionHistoryService adminCompetitionHistoryService;

    @Operation(
            summary = "대회 변경 이력",
            description = "운영자·크롤러·서버의 대회 데이터 변경 기록을 최신순으로 페이징 조회합니다. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.COMPETITION_NOT_FOUND})
    @GetMapping("/{competitionId}/history")
    public Page<ChangeHistoryResponse> getHistory(
            @Parameter(description = "대회 ID", required = true) @PathVariable Long competitionId,
            @ParameterObject @PageableDefault(size = 20) Pageable pageable) {
        return adminCompetitionHistoryService.getHistory(competitionId, pageable);
    }
}
