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
        return toContentLikeResponse(content, contentLike != null, likeCount);
    }

    /**
     * 게시물 좋아요 response — 새로 저장한 엔티티 없이 상태만 아는 경우 (동시 요청 충돌, #199)
     */
    public ContentLikeResponse toContentLikeResponse(Content content, boolean isLiked, Long likeCount) {
        return new ContentLikeResponse(
                content.getId(),
                isLiked,
                likeCount
        );
    }
}
