package com.jiujitsu.api.domain.notice.dto;

import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 목록 응답")
public record NoticeListResponse(
        @Schema(description = "알림 ID", example = "1") Long id,
        @Schema(description = "제목", example = "타이틀입니다.") String title,
        @Schema(description = "내용", example = "내용입니다.") String body,
        @Schema(description = "알림 타입", example = "NEW_COMMENTS") FcmPushType pushType,
        @Schema(description = "알림 동작 타입 (대상 컨텐츠 타입이 결정: 게시글=BOARD_DETAIL, 밸런스 게임=BALANCE_DETAIL)", example = "BOARD_DETAIL") PushActionType actionType,
        @Schema(description = "데이터값", example = "1") String data,
        @Schema(description = "읽음여부", example = "true") Boolean isRead,
        @Schema(description = "생성 일시") LocalDateTime createdAt,
        @Schema(description = "대상 삭제 상태. null=정상, BOARD=게시글 삭제, COMMENT=댓글 삭제, REPLY=대댓글 삭제",
                allowableValues = {"BOARD", "COMMENT", "REPLY"}, nullable = true)
        String deletedStatus
) {
}
