package com.jiujitsu.api.domain.community.repository.comments;

public interface ReactionCountProjection {
    Long getCommentId();
    long getLikeCnt();
    long getDislikeCnt();
}
