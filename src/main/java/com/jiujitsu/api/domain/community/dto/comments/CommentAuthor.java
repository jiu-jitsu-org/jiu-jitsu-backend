package com.jiujitsu.api.domain.community.dto.comments;

public record CommentAuthor(
        Long id,
        String nickname,
        String ProfileImageUrl
) {
}
