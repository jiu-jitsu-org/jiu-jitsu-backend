package com.jiujitsu.api.domain.community.content.factory;

import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentLike;
import org.springframework.stereotype.Component;

@Component
public class ContentLikeFactory {
    // entity 생성 클래스

    /**
     * Create ContentLike
     */
    public ContentLike createCommentLike(Content content) {
        return ContentLike.builder()
                .content(content)
                .build();
    }
}
