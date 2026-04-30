package com.jiujitsu.api.domain.community.content.mapper;

import com.jiujitsu.api.domain.community.content.dto.ContentLikeResponse;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentLike;
import org.springframework.stereotype.Component;

import java.util.Objects;

@Component
public class ContentLikeMapper {
    /**
     * 게시물 좋아요 response
     */
    public ContentLikeResponse toContentLikeResponse(Content content, ContentLike contentLike) {
        return new ContentLikeResponse(
                content.getId(),
                !Objects.isNull(contentLike)    // 좋아요가 있으면 true, 없으면 false
        );
    }
}
