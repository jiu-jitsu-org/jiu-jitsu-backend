package com.jiujitsu.api.domain.notice.dto;

import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "알림 목록 응답")
public record NoticeListResponse(
        @Schema(description = "알림 ID", example = "1") Long id,
        @Schema(description = "제목. 좋아요 알림(CONTENTS_LIKE·COMMENTS_LIKE)은 글·댓글마다 한 장이고 지금 좋아요 수가 들어간다", example = "내 게시글에 좋아요가 10개 달렸어요") String title,
        @Schema(description = "내용. 댓글·좋아요 알림 = 게시글 제목(밸런스 게임은 null), 대댓글 = 부모 댓글, 댓글 좋아요 = 좋아요 받은 댓글", example = "내용입니다.") String body,
        @Schema(description = "알림 타입", example = "NEW_COMMENTS") FcmPushType pushType,
        @Schema(description = "알림 동작 타입 (대상 타입이 결정: 게시글=BOARD_DETAIL, 밸런스 게임=BALANCE_DETAIL, 대회=COMPETITION_DETAIL)", example = "BOARD_DETAIL") PushActionType actionType,
        @Schema(description = "데이터값. 게시글·밸런스 게임 = contentId, 대회 = 대회 id (\"이 대회 알림 받지 않기\" = DELETE /competitions/{data}/notice, 이미 꺼져 있어도 같은 결과)", example = "1") String data,
        @Schema(description = "읽음여부", example = "true") Boolean isRead,
        @Schema(description = "알림 시각 (정렬 기준). 좋아요 알림은 마지막으로 푸시가 간 시각") LocalDateTime createdAt,
        @Schema(description = "대상 삭제 상태. null=정상, BOARD=게시글 삭제, COMMENT=댓글 삭제, REPLY=대댓글 삭제, COMPETITION=대회가 없거나 비공개",
                allowableValues = {"BOARD", "COMMENT", "REPLY", "COMPETITION"}, nullable = true)
        String deletedStatus
) {
}
