package com.jiujitsu.api.domain.community.balance_game.repository;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BalanceGameRepository extends JpaRepository<BalanceGame, Long> {

    // 시작했고 아직 마감되지 않은 밸런스 게임 중 가장 먼저 마감되는(=현재 진행중인) 1건
    // 미리 등록한 다음 날 게임(시작 전)은 빠진다. 마감 시각까지는 진행 중 (BalanceGame#isClosed 와 같은 경계)
    Optional<BalanceGame> findTopByStartAtLessThanEqualAndEndAtGreaterThanEqualOrderByEndAtAsc(LocalDateTime startedBy, LocalDateTime now);

    // closedAfter 이후에 마감된 게임 중 가장 최근 1건 (게임 교체 버퍼 동안 결과 노출용)
    Optional<BalanceGame> findTopByEndAtAfterAndEndAtBeforeOrderByEndAtDesc(LocalDateTime closedAfter, LocalDateTime now);

    // 상세/투표 - contentId 로 조회 (마감된 게임도 조회 가능)
    Optional<BalanceGame> findByContentId(Long contentId);
}
