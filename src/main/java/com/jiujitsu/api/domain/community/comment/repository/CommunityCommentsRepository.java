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
     * Content 다건의 상위 댓글 수 조회 (hiddenAt IS NULL, parentId가 null인 댓글만)
     */
    @Query("""
        SELECT c.content.id, COUNT(c)
        FROM CommunityComments c
        WHERE c.content.id IN :contentIds
          AND c.parentId IS NULL
          AND c.hiddenAt IS NULL
        GROUP BY c.content.id
        """)
    List<Object[]> countTopLevelCommentsByContentIds(@Param("contentIds") List<Long> contentIds);

    /**
     * Content 단건의 상위 댓글 수 조회 (hiddenAt IS NULL, parentId가 null인 댓글만)
     */
    long countByContent_IdAndParentIdIsNullAndHiddenAtIsNull(Long contentId);

    boolean existsByParentId(Long parentId);

    /**
     * 댓글 목록 조회
     * - 일반 댓글: hiddenAt IS NULL, isReported = false, deletedAt IS NULL, 차단 유저/신고한 댓글 제외
     *   - 최상위 댓글(parentId=0): 신고한 댓글이어도 포함 (대댓글 구조 유지, isReportedByMe로 구분)
     *   - 대댓글(parentId!=0): 신고한 댓글 제외
     * - soft-delete 플레이스홀더: deletedAt IS NOT NULL (유저 삭제, 대댓글 있음)
     * - reported 플레이스홀더: isReported = true (신고 자동숨김, 대댓글 있음)
     */
    @Query("""
    SELECT c FROM CommunityComments c
    LEFT JOIN FETCH c.createdBy
    LEFT JOIN FETCH c.content
    WHERE c.content.id = :contentId
      AND (
        (c.hiddenAt IS NULL AND c.isReported = false AND c.deletedAt IS NULL
         AND c.createdBy.id NOT IN :excludedAuthorIds
         AND (c.parentId = 0 OR c.id NOT IN :excludedCommentIds))
        OR c.deletedAt IS NOT NULL
        OR c.isReported = true
      )
    ORDER BY c.createdAt DESC
    """)
    List<CommunityComments> findByContentIdFiltered(
            @Param("contentId") Long contentId,
            @Param("excludedAuthorIds") Collection<Long> excludedAuthorIds,
            @Param("excludedCommentIds") Collection<Long> excludedCommentIds);

    @Query("""
    SELECT DISTINCT c.content.id
    FROM CommunityComments c
    WHERE c.createdBy.id = :userId
      AND c.content.id IN :contentIds
    """)
    Set<Long> findUserCommentedContentIds(@Param("userId") Long userId,
                                          @Param("contentIds") List<Long> contentIds);
}
