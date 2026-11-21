package com.jiujitsu.api.domain.announcement.mapper;

import com.jiujitsu.api.domain.announcement.dto.AnnouncementDetailResponse;
import com.jiujitsu.api.domain.announcement.dto.AnnouncementListResponse;
import com.jiujitsu.api.domain.announcement.entity.Announcement;
import org.springframework.stereotype.Component;

@Component
public class AnnouncementMapper {

    public AnnouncementListResponse toListResponse(Announcement announcement) {
        return new AnnouncementListResponse(announcement.getId(), announcement.getTitle(), announcement.getPublishedAt());
    }

    public AnnouncementDetailResponse toDetailResponse(Announcement announcement) {
        return new AnnouncementDetailResponse(
                announcement.getId(),
                announcement.getTitle(),
                announcement.getBody(),
                announcement.getPublishedAt()
        );
    }
}
