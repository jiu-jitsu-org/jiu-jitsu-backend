package com.jiujitsu.api.domain.notice.mapper;

import com.jiujitsu.api.domain.notice.dto.BoardNoticeSettingResponse;
import com.jiujitsu.api.domain.notice.dto.NoticeListResponse;
import com.jiujitsu.api.domain.notice.dto.NoticeSettingResponse;
import com.jiujitsu.api.domain.notice.entity.ContentNoticeSetting;
import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;

import java.util.EnumSet;
import java.util.Set;

public class NoticeMapper {
    // refId 가 댓글 ID 인 알림 (#271)
    private static final Set<FcmPushType> COMMENT_NOTICE_TYPES =
            EnumSet.of(FcmPushType.NEW_COMMENTS, FcmPushType.NEW_CHILD_COMMENTS, FcmPushType.COMMENTS_LIKE);

    // title: 좋아요 카드는 지금 좋아요 수로 다시 만든 제목 (#196), 그 밖엔 저장된 제목
    public static NoticeListResponse toNoticeListResponse(Notice notice, String title, String deletedStatus, String body) {
        return new NoticeListResponse(
                notice.getId(),
                title,
                body,
                notice.getPushType(),
                notice.getPushActionType(),
                notice.getData(),
                COMMENT_NOTICE_TYPES.contains(notice.getPushType()) ? notice.getRefId() : null,
                notice.getIsRead(),
                notice.getNotifiedAt(),
                deletedStatus
        );
    }

    public static NoticeSettingResponse toNoticeSettingResponse(UserNoticeSetting userNoticeSetting) {
        return new NoticeSettingResponse(
                userNoticeSetting.getSecurityEnabled(),
                userNoticeSetting.getServiceEnabled(),
                userNoticeSetting.getCommunityEnabled(),
                userNoticeSetting.getMarketingEnabled(),
                userNoticeSetting.getMarketingAgreedAt(),
                userNoticeSetting.getMarketingWithdrawnAt(),
                userNoticeSetting.isCompetitionEnabled(),
                userNoticeSetting.isCompetitionNoticeAsked(),
                userNoticeSetting.isBalanceResultEnabled()
        );
    }

    public static BoardNoticeSettingResponse toBoardNoticeSettingResponse(Long boardId, ContentNoticeSetting setting) {
        return new BoardNoticeSettingResponse(
                boardId,
                setting.getEnabled()
        );
    }
}
