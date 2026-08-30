package com.jiujitsu.api.domain.community.content.mapper;

import com.jiujitsu.api.domain.community.content.dto.ContentLikeResponse;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentLike;
import org.springframework.stereotype.Component;

@Component
public class ContentLikeMapper {
    /**
     * 게시물 좋아요 response
     */
    public ContentLikeResponse toContentLikeResponse(Content content, ContentLike contentLike, Long likeCount) {
        return new ContentLikeResponse(
                content.getId(),
                contentLike != null,
                likeCount
        );
    }
}
