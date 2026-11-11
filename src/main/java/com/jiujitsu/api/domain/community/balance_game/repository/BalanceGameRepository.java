package com.jiujitsu.api.domain.community.balance_game.repository;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BalanceGameRepository extends JpaRepository<BalanceGame, Long> {

    // 진행 중인 게임 (시작했고 마감 전), 먼저 마감되는 순. 미리 등록한 다음 날 게임(시작 전)은 빠진다
    // 마감 시각까지는 진행 중 (BalanceGame#isStarted·isClosed 와 같은 경계). 메인은 첫 건을 쓴다
    @Query("""
    SELECT g FROM BalanceGame g
    WHERE g.startAt <= :now AND g.endAt >= :now
    ORDER BY g.endAt ASC, g.id ASC
    """)
    List<BalanceGame> findOpen(@Param("now") LocalDateTime now, Pageable pageable);

    // 마감된 게임, 최근 마감 순 (게임 교체 버퍼 동안 결과 노출용). 메인은 첫 건을 쓴다
    // 마감 시각이 같으면 findOpen 과 같은 id ASC — 진행 중에 메인에 보인 게임의 결과가 그대로 나오게
    @Query("""
    SELECT g FROM BalanceGame g
    WHERE g.endAt < :now
    ORDER BY g.endAt DESC, g.id ASC
    """)
    List<BalanceGame> findClosed(@Param("now") LocalDateTime now, Pageable pageable);

    // 아직 시작 전인 게임, 먼저 시작하는 순 (마감된 게임 결과를 언제까지 보여 줄지 정하는 다음 게임)
    @Query("""
    SELECT g FROM BalanceGame g
    WHERE g.startAt > :now
    ORDER BY g.startAt ASC, g.id ASC
    """)
    List<BalanceGame> findUpcoming(@Param("now") LocalDateTime now, Pageable pageable);

    // 상세/투표 - contentId 로 조회 (마감된 게임도 조회 가능)
    Optional<BalanceGame> findByContentId(Long contentId);
}
