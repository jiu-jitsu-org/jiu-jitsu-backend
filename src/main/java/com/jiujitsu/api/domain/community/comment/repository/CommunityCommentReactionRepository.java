package com.jiujitsu.api.domain.community.comment.repository;

import com.jiujitsu.api.domain.community.comment.entity.CommunityCommentReaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityCommentReactionRepository extends JpaRepository<CommunityCommentReaction, Long> {

    @Query(value = """
                select *
                from community_comment_reaction
                where comment_id = :commentId
                  and user_id = :userId
                limit 1
            """, nativeQuery = true)
    Optional<CommunityCommentReaction> findByCommentAndUser(
            @Param("commentId") Long commentId,
            @Param("userId") Long userId
    );

    @Query(value = """
                select *
                from community_comment_reaction
                where user_id = :userId
            """, nativeQuery = true)
    List<CommunityCommentReaction> findByUser(
            @Param("userId") Long userId
    );

    // COUNT
    @Query(value = """
                select count(*)
                from community_comment_reaction
                where comment_id = :commentId
                  and reaction_type = :type
            """, nativeQuery = true)
    long countByCommentAndReactionType(
            @Param("commentId") Long commentId,
            @Param("type") String type // 네이티브에서는 문자열로 넘기는 게 안전 ("LIKE"/"DISLIKE")
    );

    @Query("""
                select r.comment.id as commentId, r.reactionType as reactionType, count(r) as cnt
                from CommunityCommentReaction r
                where r.comment.id in :commentIds
                group by r.comment.id, r.reactionType
            """)
    List<ReactionCountProjection> countByCommentIdsGroupByType(@Param("commentIds") Collection<Long> commentIds);
}
