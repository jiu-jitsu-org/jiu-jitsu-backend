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
    // mentionCommentId: 대댓글의 답글 버튼으로 쓴 답글이면 그 대댓글 id, 아니면 null (#216)
    public CommunityComments createComments(Content content, Long parentId, String body, Long mentionCommentId) {
        return CommunityComments.builder()
                .content(content)
                .parentId(parentId)
                .body(body)
                .mentionCommentId(mentionCommentId)
                .build();
    }
}
