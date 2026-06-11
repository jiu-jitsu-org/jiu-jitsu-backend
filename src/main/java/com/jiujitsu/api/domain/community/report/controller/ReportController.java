package com.jiujitsu.api.domain.community.report.controller;

import com.jiujitsu.api.domain.community.report.dto.ReportCreateRequest;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "report-controller", description = "신고 API")
@CommonApiResponses
@RestController
@RequestMapping("/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "게시물/댓글 신고", description = "게시물 또는 댓글을 신고합니다. 동일 대상 중복 신고 불가.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({
            ErrorCode.ALREADY_REPORTED,
            ErrorCode.SELF_REPORT_NOT_ALLOWED,
            ErrorCode.BOARD_NOT_FOUND,
            ErrorCode.COMMENT_NOT_FOUND
    })
    @PostMapping
    public void createReport(@Valid @RequestBody ReportCreateRequest request) {
        reportService.createReport(request);
    }
}
