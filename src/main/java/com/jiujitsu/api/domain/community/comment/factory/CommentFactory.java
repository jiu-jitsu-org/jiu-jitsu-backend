package com.jiujitsu.api.domain.community.comment.factory;

import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.content.entity.Content;
import org.springframework.stereotype.Component;

@Component
public class CommentFactory {
    // entity 생성 클래스

    /**
     * Create Comments
     */
    public CommunityComments createComments(Content content, Long parentId, String body) {
        return CommunityComments.builder()
                .content(content)
                .parentId(parentId == 0L ? null : parentId)
                .body(body)
                .build();
    }
}
