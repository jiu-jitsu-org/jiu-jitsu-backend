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
        @Schema(description = "알림 동작 타입", example = "BOARD_DETAIL") PushActionType actionType,
        @Schema(description = "데이터값", example = "1") String data,
        @Schema(description = "읽음여부", example = "true") Boolean isRead,
        @Schema(description = "생성 일시") LocalDateTime createdAt
) {
}
