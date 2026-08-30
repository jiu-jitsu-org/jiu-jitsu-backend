package com.jiujitsu.api.domain.community.comment.repository;

import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
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
     * - 일반 댓글: hiddenAt IS NULL, isReported = false, deletedAt IS NULL
     *   - 차단 유저 댓글도 포함 (isBlocked로 구분, 스레드 구조 유지)
     *   - 댓글/대댓글 모두 신고해도 포함 (isReportedByMe로 구분)
     * - soft-delete 플레이스홀더: deletedAt IS NOT NULL (유저 삭제, 대댓글 있음)
     * - reported 플레이스홀더: isReported = true (신고 자동숨김, 대댓글 있거나 대댓글 자신)
     */
    @Query("""
    SELECT c FROM CommunityComments c
    LEFT JOIN FETCH c.createdBy
    LEFT JOIN FETCH c.content
    WHERE c.content.id = :contentId
      AND (
        (c.hiddenAt IS NULL AND c.isReported = false AND c.deletedAt IS NULL)
        OR c.deletedAt IS NOT NULL
        OR c.isReported = true
      )
    ORDER BY c.createdAt DESC, c.id DESC
    """)
    List<CommunityComments> findByContentIdFiltered(@Param("contentId") Long contentId);

    /**
     * 대댓글 추가 조회 (parentId 기준, 숨김 필터는 목록 조회와 동일)
     */
    @EntityGraph(attributePaths = {"createdBy", "content"})
    @Query("""
    SELECT c FROM CommunityComments c
    WHERE c.parentId = :parentId
      AND (
        (c.hiddenAt IS NULL AND c.isReported = false AND c.deletedAt IS NULL)
        OR c.deletedAt IS NOT NULL
        OR c.isReported = true
      )
    """)
    List<CommunityComments> findRepliesByParentId(@Param("parentId") Long parentId, Pageable pageable);

    @Query("""
    SELECT DISTINCT c.content.id
    FROM CommunityComments c
    WHERE c.createdBy.id = :userId
      AND c.content.id IN :contentIds
    """)
    Set<Long> findUserCommentedContentIds(@Param("userId") Long userId,
                                          @Param("contentIds") List<Long> contentIds);

    @Query("""
    SELECT DISTINCT c.parentId
    FROM CommunityComments c
    WHERE c.createdBy.id = :userId
      AND c.parentId IN :parentIds
    """)
    Set<Long> findRepliedParentIds(@Param("userId") Long userId,
                                   @Param("parentIds") Collection<Long> parentIds);
}
