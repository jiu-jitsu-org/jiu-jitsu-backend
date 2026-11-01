package com.jiujitsu.api.domain.community.board.repository;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BoardRepository extends JpaRepository<Board, Long> {

    Optional<Board> findByContent_Id(Long contentId);

    @Query("SELECT b FROM Board b WHERE b.content.id IN :contentIds")
    List<Board> findAllByContentIdIn(@Param("contentIds") List<Long> contentIds);

    @Query("""
    SELECT b FROM Board b
    WHERE b.category.id = :categoryId
      AND b.hiddenAt IS NULL
      AND b.createdBy.id NOT IN :excludedAuthorIds
      AND b.content.id NOT IN :excludedBoardContentIds
    """)
    Page<Board> findAllByCategoryFiltered(
            @Param("categoryId") Long categoryId,
            @Param("excludedAuthorIds") List<Long> excludedAuthorIds,
            @Param("excludedBoardContentIds") List<Long> excludedBoardContentIds,
            Pageable pageable);

    @Query("""
    SELECT b FROM Board b
    WHERE (b.title LIKE CONCAT('%', :keyword, '%') OR b.body LIKE CONCAT('%', :keyword, '%'))
      AND b.hiddenAt IS NULL
      AND b.createdBy.id NOT IN :excludedAuthorIds
      AND b.content.id NOT IN :excludedBoardContentIds
    """)
    Page<Board> findByTitleBodyKeywordFiltered(
            @Param("keyword") String keyword,
            @Param("excludedAuthorIds") List<Long> excludedAuthorIds,
            @Param("excludedBoardContentIds") List<Long> excludedBoardContentIds,
            Pageable pageable);

    @Query("""
    SELECT b FROM Board b
    WHERE b.hiddenAt IS NULL
      AND b.createdBy.id NOT IN :excludedAuthorIds
      AND b.content.id NOT IN :excludedBoardContentIds
    """)
    Page<Board> findAllFiltered(
            @Param("excludedAuthorIds") List<Long> excludedAuthorIds,
            @Param("excludedBoardContentIds") List<Long> excludedBoardContentIds,
            Pageable pageable);

    @Query("""
    SELECT b FROM Board b
    WHERE b.createdBy = :createdBy
      AND b.hiddenAt IS NULL
    """)
    Page<Board> findAllByCreatedByAndNotHidden(@Param("createdBy") User createdBy, Pageable pageable);

    /**
     * 특정 사용자가 작성한 글 목록 (타인 조회용)
     * - 숨김 · 신고 콘텐츠 제외
     * - 뷰어가 차단한 작성자 제외(자기 글 조회 시에는 빈 리스트 전달)
     */
    @Query("""
    SELECT b FROM Board b
    WHERE b.createdBy.id = :authorId
      AND b.hiddenAt IS NULL
      AND b.content.id NOT IN :excludedContentIds
    """)
    Page<Board> findAllByUserFiltered(
            @Param("authorId") Long authorId,
            @Param("excludedContentIds") List<Long> excludedContentIds,
            Pageable pageable);

    @Query("""
    SELECT b
    FROM ContentSave cs
    JOIN cs.content c
    JOIN Board b ON b.content = c
    WHERE cs.createdBy.id = :userId
      AND b.hiddenAt IS NULL
    """)
    Page<Board> findSavedBoardsNotHidden(@Param("userId") Long userId, Pageable pageable);
}
