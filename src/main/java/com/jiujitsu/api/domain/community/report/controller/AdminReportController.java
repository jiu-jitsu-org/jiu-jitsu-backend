package com.jiujitsu.api.domain.community.report.controller;

import com.jiujitsu.api.domain.community.report.dto.AdminReportListResponse;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.security.annotation.AdminOnly;
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
@AdminOnly
@RestController
@RequestMapping("/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final ReportService reportService;

    @Operation(summary = "신고 목록 조회", description = "전체 신고 목록을 페이징 조회합니다.<br>"
            + "- BOARD: targetId = contentId, targetPreview = 게시글 제목 + 본문, isHidden = 숨김 여부<br>"
            + "- COMMENT: targetId = commentId, targetPreview = 댓글 내용, isHidden = 숨김 여부<br>"
            + "- USER (회원 신고): targetId = userId, targetPreview = 대상 닉네임, isHidden 은 항상 false (자동 숨김·숨김 해제 없음)<br>"
            + "totalReportCount 는 같은 대상이 받은 신고 수")
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

    // ── 신고 ID 기반 처리 ───────────────────────────────────────────

    @Operation(
            summary = "신고 대상 숨김",
            description = "신고 ID로 대상 게시글·댓글을 숨김 처리합니다. 작성자 위반 처리 수가 1 증가합니다. USER 신고에는 사용할 수 없습니다."
    )
    @ApiErrorCodeExamples({ErrorCode.REPORT_NOT_FOUND, ErrorCode.BOARD_NOT_FOUND, ErrorCode.COMMENT_NOT_FOUND, ErrorCode.WRONG_PARAMETER})
    @PatchMapping("/{reportId}/hide")
    public void hide(
            @Parameter(description = "신고 ID") @PathVariable Long reportId
    ) {
        reportService.hide(reportId);
    }

    @Operation(
            summary = "신고 대상 삭제",
            description = "신고 ID로 대상 게시글·댓글을 삭제합니다. 댓글은 대댓글이 있으면 soft-delete, 없으면 hard-delete. 작성자 위반 처리 수가 1 증가합니다. USER 신고에는 사용할 수 없습니다."
    )
    @ApiErrorCodeExamples({ErrorCode.REPORT_NOT_FOUND, ErrorCode.BOARD_NOT_FOUND, ErrorCode.COMMENT_NOT_FOUND, ErrorCode.WRONG_PARAMETER})
    @DeleteMapping("/{reportId}")
    public void delete(
            @Parameter(description = "신고 ID") @PathVariable Long reportId
    ) {
        reportService.deleteTarget(reportId);
    }

    @Operation(
            summary = "신고 반려 (숨김 해제)",
            description = "신고 ID로 대상 게시글·댓글의 숨김을 해제합니다. 위반 처리 수는 변경하지 않습니다. USER 신고에는 사용할 수 없습니다."
    )
    @ApiErrorCodeExamples({ErrorCode.REPORT_NOT_FOUND, ErrorCode.BOARD_NOT_FOUND, ErrorCode.COMMENT_NOT_FOUND, ErrorCode.WRONG_PARAMETER})
    @PatchMapping("/{reportId}/reject")
    public void reject(
            @Parameter(description = "신고 ID") @PathVariable Long reportId
    ) {
        reportService.reject(reportId);
    }
}
