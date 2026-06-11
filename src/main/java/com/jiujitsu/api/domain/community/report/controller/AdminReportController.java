package com.jiujitsu.api.domain.community.report.controller;

import com.jiujitsu.api.domain.community.report.dto.AdminReportListResponse;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@Tag(name = "admin-report-controller", description = "관리자 신고 관리 API")
@CommonApiResponses
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "신고 목록 조회", description = "전체 신고 목록을 페이징으로 조회합니다.")
    @GetMapping
    public Page<AdminReportListResponse> getReports(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        return reportService.getAdminReports(pageable);
    }

    @Operation(summary = "게시물 숨김 해제", description = "신고로 숨김 처리된 게시물을 숨김 해제합니다.")
    @ApiErrorCodeExamples({ErrorCode.BOARD_NOT_FOUND})
    @PatchMapping("/board/{contentId}/unhide")
    public void unhideBoard(
            @Parameter(name = "contentId", description = "게시물 content ID", required = true)
            @PathVariable Long contentId
    ) {
        reportService.unhideBoard(contentId);
    }

    @Operation(summary = "댓글 숨김 해제", description = "신고로 숨김 처리된 댓글을 숨김 해제합니다.")
    @ApiErrorCodeExamples({ErrorCode.COMMENT_NOT_FOUND})
    @PatchMapping("/comment/{commentId}/unhide")
    public void unhideComment(
            @Parameter(name = "commentId", description = "댓글 ID", required = true)
            @PathVariable Long commentId
    ) {
        reportService.unhideComment(commentId);
    }
}
