package com.jiujitsu.api.domain.community.comment.dto;

public record CommentAuthor(
        Long id,
        String nickname,
        String ProfileImageUrl
) {
}
