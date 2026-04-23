package com.jiujitsu.api.domain.community.comment.repository;

import com.jiujitsu.api.domain.community.comment.entity.CommentLike;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    Optional<CommentLike> findByCommentIdAndCreatedBy(Long commentId, User user);

    // 좋아요 개수
    @Query("select cl.comment.id, count(cl) from CommentLike cl where cl.comment.id in :commentIds group by cl.comment.id")
    List<Object[]> countGroupByCommentIds(@Param("commentIds") List<Long> commentIds);

    // 내가 누른 좋아요
    @Query("select cl.comment.id from CommentLike cl where cl.comment.id in :commentIds and cl.createdBy.id = :userId")
    List<Long> findLikedCommentIds(@Param("commentIds") List<Long> commentIds,
                                   @Param("userId") Long userId);
}