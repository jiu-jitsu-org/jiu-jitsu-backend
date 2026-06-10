package com.jiujitsu.api.domain.community.content.factory;

import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentSave;
import org.springframework.stereotype.Component;

@Component
public class ContentSaveFactory {
    // entity 생성 클래스

    /**
     * Create ContentSave
     */
    public ContentSave createCommentSave(Content content) {
        return ContentSave.builder()
                .content(content)
                .build();
    }
}
