package com.jiujitsu.api.domain.community.comment.repository;

import com.jiujitsu.api.domain.community.comment.entity.CommentLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentLikeRepository extends JpaRepository<CommentLike, Long> {

    // 단건 조회 대신 존재 여부·벌크 삭제를 쓴다: 제약이 생기기 전 중복 행이 있어도, 동시에 취소해 지울 행이 없어도 예외가 나지 않는다 (#199)
    boolean existsByCommentIdAndCreatedById(Long commentId, Long userId);

    // 벌크 삭제로 둔다: 파생 deleteBy 는 엔티티를 읽어 한 건씩 지워, 동시 취소로 먼저 지워진 행이면 예외가 난다
    @Modifying
    @Query("delete from CommentLike cl where cl.comment.id = :commentId and cl.createdBy.id = :userId")
    void deleteAllByCommentIdAndCreatedById(@Param("commentId") Long commentId, @Param("userId") Long userId);

    // 좋아요 개수
    @Query("select cl.comment.id, count(cl) from CommentLike cl where cl.comment.id in :commentIds group by cl.comment.id")
    List<Object[]> countGroupByCommentIds(@Param("commentIds") List<Long> commentIds);

    // 내가 누른 좋아요
    @Query("select cl.comment.id from CommentLike cl where cl.comment.id in :commentIds and cl.createdBy.id = :userId")
    List<Long> findLikedCommentIds(@Param("commentIds") List<Long> commentIds,
                                   @Param("userId") Long userId);
}