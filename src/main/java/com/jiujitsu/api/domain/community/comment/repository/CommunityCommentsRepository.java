package com.jiujitsu.api.domain.community.comment.repository;

import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.content.entity.Content;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Objects;

@Repository
public interface CommunityCommentsRepository extends JpaRepository<CommunityComments, Long> {

    /**
     * 댓글 작성
     * @param content  Content 엔티티
     * @param parentId 부모 댓글 ID
     * @param body     댓글내용
     * @return 댓글
     */
    default CommunityComments createComment(
            Content content,
            Long parentId,
            String body
    ) {
        Objects.requireNonNull(content, "content must not be null");
        Objects.requireNonNull(body, "body must not be null");

        CommunityComments comment = CommunityComments.builder()
                .content(content)
                .parentId(parentId)
                .body(body)
                .build();
        return save(comment);
    }

    /**
     * 상위 댓글만 조회
     * @param contentId Content ID
     * @return 댓글 리스트
     */
    @Query("""
        select c
        from CommunityComments c
        join fetch c.content
        where c.content.id = :contentId
            and c.parentId is null
        order by c.createdAt asc, c.id asc
   \s""")
    List<CommunityComments> findCommentsByContentId(@Param("contentId") Long contentId);

    /**
     * 하위 댓글만 조회
     * @param contentId Content ID
     * @param parentId  부모 댓글 ID
     * @return 댓글 리스트
     */
    @Query("""
        select c
        from CommunityComments c
        join fetch c.content
        where c.content.id = :contentId
            and c.parentId = :parentId
        order by c.createdAt asc, c.id asc
   \s""")
    List<CommunityComments> findByContentIdAndParentIdOrderByCreatedAtAsc(
            @Param("contentId") Long contentId,
            @Param("parentId") Long parentId
    );

    /**
     * Content별 상위 댓글 수 조회 (parentId가 null인 댓글만, 대댓글 제외)
     * @param contentIds Content ID 목록
     * @return [contentId, count] 형태의 리스트
     */
    @Query("""
        select c.content.id, count(c)
        from CommunityComments c
        where c.content.id in :contentIds
            and c.parentId is null
        group by c.content.id
   \s""")
    List<Object[]> countTopLevelCommentsByContentIds(@Param("contentIds") List<Long> contentIds);

    /**
     * Content의 상위 댓글 수 조회 (parentId가 null인 댓글만, 대댓글 제외)
     */
    long countByContent_IdAndParentIdIsNull(Long content_Id);

}
