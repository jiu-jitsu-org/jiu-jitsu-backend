package com.jiujitsu.api.domain.notice.controller;

import com.jiujitsu.api.domain.notice.dto.BoardNoticeSettingResponse;
import com.jiujitsu.api.domain.notice.dto.CompetitionNoticePromptRequest;
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
import jakarta.validation.Valid;
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

    @Operation(summary = "안 읽은 알림 수 조회", description = "안 읽은 알림 수를 반환합니다. 종 아이콘 dot 갱신용으로, 목록 조회와 동일한 집계 기준을 사용합니다.")
    @LoginErrorExamples
    @GetMapping("/unread-count")
    public long getUnreadCount() {
        return noticeService.getUnreadCount();
    }

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

    @Operation(
            summary = "관심 대회 알림 질문 답 저장",
            description = "처음 관심 등록 때 묻는 \"이 대회 마감 알림을 받을까요?\"의 답을 저장합니다. 응답은 알림 설정 전체\n\n"
                    + "- 앱은 관심 등록 때 competitionEnabled = false 이고 competitionNoticeAsked = false 일 때만 묻습니다 (설정에서 이미 켠 사람에겐 묻지 않음)\n"
                    + "- 알림 받기 = enabled true: 관심 대회 알림(competitionEnabled)을 켭니다\n"
                    + "- 나중에 = enabled false: 관심 대회 알림은 그대로(꺼진 채) 둡니다. 켜져 있었다면 끄지 않습니다\n"
                    + "- 어느 쪽이든 competitionNoticeAsked 가 true 가 되어 다시 묻지 않습니다 (기기를 바꿔도)"
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.REQUIRED_PARAMETER})
    @PutMapping("/setting/competition")
    public NoticeSettingResponse answerCompetitionNoticePrompt(@Valid @RequestBody CompetitionNoticePromptRequest request) {
        return noticeService.answerCompetitionNoticePrompt(request);
    }

    @Operation(summary = "알림 설정 조회", description = "알림 타입별 수신여부를 조회합니다.")
    @LoginErrorExamples
    @GetMapping("/setting")
    public NoticeSettingResponse getNoticeSetting() {
        return noticeService.getNoticeSetting();
    }

    @Operation(summary = "게시물 알림 설정 조회", description = "특정 컨텐츠의 댓글/좋아요 알림 설정을 조회합니다. 컨텐츠 단위 기능이라 밸런스 게임의 contentId 로도 호출할 수 있습니다.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.CONTENT_NOT_FOUND})
    @GetMapping("/setting/board/{boardId}")
    public BoardNoticeSettingResponse getBoardNoticeSetting(
            @Parameter(name = "boardId", description = "컨텐츠 ID (게시글/밸런스 게임 공통)", required = true) @PathVariable Long boardId
    ) {
        return noticeService.getBoardNoticeSetting(boardId);
    }

    @Operation(summary = "게시물 알림 설정 토글", description = "특정 컨텐츠의 알림(댓글/좋아요/게시물 전체)을 켜거나 끕니다. 컨텐츠 단위 기능이라 밸런스 게임의 contentId 로도 호출할 수 있습니다.")
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.CONTENT_NOT_FOUND})
    @PutMapping("/setting/board/{boardId}")
    public BoardNoticeSettingResponse toggleBoardNoticeSetting(
            @Parameter(name = "boardId", description = "컨텐츠 ID (게시글/밸런스 게임 공통)", required = true) @PathVariable Long boardId
    ) {
        return noticeService.toggleBoardNoticeSetting(boardId);
    }
}
