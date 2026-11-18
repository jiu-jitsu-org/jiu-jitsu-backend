package com.jiujitsu.api.domain.community.board.repository;

import com.jiujitsu.api.domain.community.board.dto.AdminSearchMissResponse;
import com.jiujitsu.api.domain.community.board.entity.BoardSearchMiss;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface BoardSearchMissRepository extends JpaRepository<BoardSearchMiss, Long> {

    // 기간 안의 0건 검색어를 많이 나온 순(같으면 최근 순)으로
    @Query("""
    SELECT new com.jiujitsu.api.domain.community.board.dto.AdminSearchMissResponse(m.keyword, COUNT(m), MAX(m.searchedAt))
    FROM BoardSearchMiss m
    WHERE m.searchedAt >= :from AND m.searchedAt < :to
    GROUP BY m.keyword
    ORDER BY COUNT(m) DESC, MAX(m.searchedAt) DESC
    """)
    List<AdminSearchMissResponse> findTopKeywords(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);
}
