package com.jiujitsu.api.domain.notice.controller;

import com.jiujitsu.api.domain.notice.dto.BoardNoticeSettingResponse;
import com.jiujitsu.api.domain.notice.dto.NoticeListResponse;
import com.jiujitsu.api.domain.notice.dto.NoticeSettingRequest;
import com.jiujitsu.api.domain.notice.dto.NoticeSettingResponse;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "notice-controller", description = "알림 API")
@CommonApiResponses
@RestController
@RequestMapping("/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @Operation(summary = "알림 목록 조회", description = "알림 목록을 조회합니다.")
    @LoginErrorExamples
    @GetMapping
    public List<NoticeListResponse> getCategory() {
        return noticeService.getNoticeList();
    }

    @Operation(summary = "알림 모두 읽음 처리", description = "모든 알림을 읽음 처리합니다.")
    @LoginErrorExamples
    @PutMapping
    public Boolean readAllNotice() {
        return noticeService.readAllNotice();
    }

    @Operation(summary = "알림 개별 읽음 처리", description = "개별 알림을 읽음 처리합니다.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.NOTICE_NOT_FOUND, ErrorCode.PERMISSION_DENIED})
    @PutMapping("/{id}")
    public Boolean readNotice(
            @Parameter(name = "id", description = "알림 ID", required = true) @PathVariable(name = "id") Long id
    ) {
        return noticeService.readNotice(id);
    }

    @Operation(summary = "알림 수신 설정", description = "알림 타입별로 수신여부 설정합니다.")
    @LoginErrorExamples
    @PostMapping("/setting")
    public NoticeSettingResponse settingNotice(@RequestBody NoticeSettingRequest request) {
        return noticeService.saveNoticeSetting(request);
    }

    @Operation(summary = "게시물 알림 설정 조회", description = "특정 게시물의 댓글/좋아요 알림 설정을 조회합니다.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.BOARD_NOT_FOUND})
    @GetMapping("/setting/board/{boardId}")
    public BoardNoticeSettingResponse getBoardNoticeSetting(
            @Parameter(name = "boardId", description = "게시글 ID", required = true) @PathVariable Long boardId
    ) {
        return noticeService.getBoardNoticeSetting(boardId);
    }

    @Operation(summary = "게시물 알림 설정 토글", description = "특정 게시물의 알림(댓글/좋아요/게시물 전체)을 켜거나 끕니다.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.BOARD_NOT_FOUND})
    @PutMapping("/setting/board/{boardId}")
    public BoardNoticeSettingResponse toggleBoardNoticeSetting(
            @Parameter(name = "boardId", description = "게시글 ID", required = true) @PathVariable Long boardId
    ) {
        return noticeService.toggleBoardNoticeSetting(boardId);
    }
}
