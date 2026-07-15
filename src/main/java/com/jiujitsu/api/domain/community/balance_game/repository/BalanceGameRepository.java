package com.jiujitsu.api.domain.community.balance_game.repository;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public interface BalanceGameRepository extends JpaRepository<BalanceGame, Long> {

    // 아직 마감되지 않은 밸런스 게임 중 가장 먼저 마감되는(=현재 진행중인) 1건
    Optional<BalanceGame> findTopByEndAtAfterOrderByEndAtAsc(LocalDateTime now);

    // 상세/투표 - contentId 로 조회 (마감된 게임도 조회 가능)
    Optional<BalanceGame> findByContentId(Long contentId);
}
