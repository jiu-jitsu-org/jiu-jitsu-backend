package com.jiujitsu.api.domain.notice.mapper;

import com.jiujitsu.api.domain.notice.dto.BoardNoticeSettingResponse;
import com.jiujitsu.api.domain.notice.dto.NoticeListResponse;
import com.jiujitsu.api.domain.notice.dto.NoticeSettingResponse;
import com.jiujitsu.api.domain.notice.entity.ContentNoticeSetting;
import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;

public class NoticeMapper {
    public static NoticeListResponse toNoticeListResponse(Notice notice) {
        return new NoticeListResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.getPushType(),
                notice.getPushActionType(),
                notice.getData(),
                notice.getIsRead(),
                notice.getCreatedAt()
        );
    }

    public static NoticeSettingResponse toNoticeSettingResponse(UserNoticeSetting userNoticeSetting) {
        return new NoticeSettingResponse(
                userNoticeSetting.getSecurityEnabled(),
                userNoticeSetting.getServiceEnabled(),
                userNoticeSetting.getCommunityEnabled(),
                userNoticeSetting.getMarketingEnabled()
        );
    }

    public static BoardNoticeSettingResponse toBoardNoticeSettingResponse(Long boardId, ContentNoticeSetting setting) {
        return new BoardNoticeSettingResponse(
                boardId,
                setting.getEnabled()
        );
    }
}
