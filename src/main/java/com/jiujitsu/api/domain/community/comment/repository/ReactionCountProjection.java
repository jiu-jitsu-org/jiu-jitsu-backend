package com.jiujitsu.api.domain.community.comment.repository;

public interface ReactionCountProjection {
    Long getCommentId();
    long getLikeCnt();
    long getDislikeCnt();
}
