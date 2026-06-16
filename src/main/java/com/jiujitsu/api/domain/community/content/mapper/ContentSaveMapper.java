package com.jiujitsu.api.domain.community.content.mapper;

import com.jiujitsu.api.domain.community.content.dto.ContentSaveResponse;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentSave;
import org.springframework.stereotype.Component;

@Component
public class ContentSaveMapper {
    /**
     * 게시물 저장 response
     */
    public ContentSaveResponse toContentSaveResponse(Content content, ContentSave contentSave) {
        return new ContentSaveResponse(
                content.getId(),
                contentSave != null
        );
    }
}
