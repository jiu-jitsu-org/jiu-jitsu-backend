package com.jiujitsu.api.domain.community.comment.repository;

import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
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
     * @param postId   게시글ID
     * @param parentId  부모 댓글 ID
     * @param body      댓글내용
     * @return  댓글
     */
    default CommunityComments createComment(
            Long postId,
            Long parentId,
            String body
    ) {
        Objects.requireNonNull(postId, "postId must not be null");
        Objects.requireNonNull(body, "body must not be null");

        CommunityComments comment = CommunityComments.builder()
                .postId(postId)
                .parentId(parentId)
                .body(body)
                .build();
        return save(comment);
    }

    /**
     * 상위 댓글만 조회
     * @param postId 게시글ID
     * @return 댓글 리스트
     */
    @Query("""
        select c
        from CommunityComments c
        where c.postId = :postId
            and c.parentId is null
        order by c.createdAt asc, c.id asc
   \s""")
    List<CommunityComments> findCommentsByPostId(@Param("postId") Long postId);

    /**
     * 하위 댓글만 조회
     * @param postId  게시글ID
     * @param parentId 부모 댓글 ID
     * @return  댓글 리스트
     */
    @Query("""
        select c
        from CommunityComments c
        where c.postId = :postId
            and c.parentId = :parentId
        order by c.createdAt asc, c.id asc
   \s""")
    List<CommunityComments> findByPostIdAndParentIdOrderByCreatedAtAsc(
            @Param("postId") Long postId,
            @Param("parentId") Long parentId
    );

}
