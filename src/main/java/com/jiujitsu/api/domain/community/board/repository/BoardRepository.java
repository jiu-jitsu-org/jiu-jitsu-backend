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

    Page<Board> findAllByCategory_Id(Long categoryId, Pageable pageable);
    Optional<Board> findByContent_Id(Long contentId);

    @Query("""
            select b
            from Board b
            where b.title like %:keyword%
            or b.body like %:keyword%
            """)
    Page<Board> findByTitleBodyKeyword(@Param("keyword") String keyword, Pageable pageable);

    Page<Board> findAllByCreatedBy(User createdBy, Pageable pageable);

    @Query("""
    select b
    from ContentSave cs
    join cs.content c
    join Board b on b.content = c
    where cs.createdBy.id = :userId
    """)
    Page<Board> findSavedBoards(@Param("userId") Long userId, Pageable pageable);

    @Query("""
    select b from Board b
    where b.category.id = :categoryId
    and b.createdBy.id not in :blockedUserIds
    """)
    Page<Board> findAllByCategoryExcludingBlockedUsers(
            @Param("categoryId") Long categoryId,
            @Param("blockedUserIds") List<Long> blockedUserIds,
            Pageable pageable);

    @Query("""
    select b from Board b
    where (b.title like %:keyword% or b.body like %:keyword%)
    and b.createdBy.id not in :blockedUserIds
    """)
    Page<Board> findByTitleBodyKeywordExcludingBlockedUsers(
            @Param("keyword") String keyword,
            @Param("blockedUserIds") List<Long> blockedUserIds,
            Pageable pageable);

    @Query("""
    select b from Board b
    where b.createdBy.id not in :blockedUserIds
    """)
    Page<Board> findAllExcludingBlockedUsers(
            @Param("blockedUserIds") List<Long> blockedUserIds,
            Pageable pageable);
}
