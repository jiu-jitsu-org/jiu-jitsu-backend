package com.jiujitsu.api.domain.community.comment.repository;

import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Set;

@Repository
public interface CommunityCommentsRepository extends JpaRepository<CommunityComments, Long> {

    /**
     * Content 다건의 상위 댓글 수 조회 (hiddenAt IS NULL, deletedAt IS NULL, parentId가 null인 댓글만)
     */
    @Query("""
        SELECT c.content.id, COUNT(c)
        FROM CommunityComments c
        WHERE c.content.id IN :contentIds
          AND c.parentId IS NULL
          AND c.hiddenAt IS NULL
          AND c.deletedAt IS NULL
        GROUP BY c.content.id
        """)
    List<Object[]> countTopLevelCommentsByContentIds(@Param("contentIds") List<Long> contentIds);

    /**
     * Content 단건의 상위 댓글 수 조회 (hiddenAt IS NULL, deletedAt IS NULL, parentId가 null인 댓글만)
     */
    long countByContent_IdAndParentIdIsNullAndHiddenAtIsNullAndDeletedAtIsNull(Long contentId);

    /**
     * 댓글 목록 조회 (hiddenAt IS NULL, 차단 유저 + 신고한 댓글 제외)
     * soft-deleted 댓글은 필터링 없이 포함 (대댓글 컨텍스트 유지용)
     */
    @Query("""
    SELECT c FROM CommunityComments c
    LEFT JOIN FETCH c.createdBy
    LEFT JOIN FETCH c.content
    WHERE c.content.id = :contentId
      AND c.hiddenAt IS NULL
      AND (
        c.deletedAt IS NOT NULL
        OR (
          c.createdBy.id NOT IN :excludedAuthorIds
          AND c.id NOT IN :excludedCommentIds
        )
      )
    ORDER BY c.createdAt DESC
    """)
    List<CommunityComments> findByContentIdFiltered(
            @Param("contentId") Long contentId,
            @Param("excludedAuthorIds") Collection<Long> excludedAuthorIds,
            @Param("excludedCommentIds") Collection<Long> excludedCommentIds);

    boolean existsByParentId(Long parentId);

    @Query("""
    SELECT DISTINCT c.content.id
    FROM CommunityComments c
    WHERE c.createdBy.id = :userId
      AND c.content.id IN :contentIds
    """)
    Set<Long> findUserCommentedContentIds(@Param("userId") Long userId,
                                          @Param("contentIds") List<Long> contentIds);
}
