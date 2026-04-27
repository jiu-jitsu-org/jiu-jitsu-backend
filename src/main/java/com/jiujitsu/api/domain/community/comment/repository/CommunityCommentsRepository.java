package com.jiujitsu.api.domain.community.comment.repository;

import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityCommentsRepository extends JpaRepository<CommunityComments, Long> {

    /**
     * Content 다건의 상위 댓글 수 조회 (parentId가 null인 댓글만, 대댓글 제외)
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
     * Content 단건의 상위 댓글 수 조회 (parentId가 null인 댓글만, 대댓글 제외)
     */
    long countByContent_IdAndParentIdIsNull(Long content_Id);

    /**
     * contents의 전체 댓글 조회(댓글 + 대댓글)
     */
    List<CommunityComments> findByContentIdOrderByCreatedAtDesc(Long contentId);
}
