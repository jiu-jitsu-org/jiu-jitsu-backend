package com.jiujitsu.api.domain.notice.mapper;

import com.jiujitsu.api.domain.notice.dto.BoardNoticeSettingResponse;
import com.jiujitsu.api.domain.notice.dto.NoticeListResponse;
import com.jiujitsu.api.domain.notice.dto.NoticeSettingResponse;
import com.jiujitsu.api.domain.notice.entity.ContentNoticeSetting;
import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;

public class NoticeMapper {
    // title: 좋아요 카드는 지금 좋아요 수로 다시 만든 제목 (#196), 그 밖엔 저장된 제목
    public static NoticeListResponse toNoticeListResponse(Notice notice, String title, String deletedStatus, String body) {
        return new NoticeListResponse(
                notice.getId(),
                title,
                body,
                notice.getPushType(),
                notice.getPushActionType(),
                notice.getData(),
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
                userNoticeSetting.isCompetitionEnabled(),
                userNoticeSetting.isCompetitionNoticeAsked()
        );
    }

    public static BoardNoticeSettingResponse toBoardNoticeSettingResponse(Long boardId, ContentNoticeSetting setting) {
        return new BoardNoticeSettingResponse(
                boardId,
                setting.getEnabled()
        );
    }
}
