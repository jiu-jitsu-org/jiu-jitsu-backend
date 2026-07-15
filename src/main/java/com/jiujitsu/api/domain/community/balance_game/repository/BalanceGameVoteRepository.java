package com.jiujitsu.api.domain.community.balance_game.repository;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface BalanceGameVoteRepository extends JpaRepository<BalanceGameVote, Long> {

    // 선택지별 득표수
    long countByBalanceGameIdAndOption(Long balanceGameId, BalanceGameOption option);

    // 현재 사용자의 투표 (myVote / 토글 판단)
    Optional<BalanceGameVote> findByBalanceGameIdAndCreatedBy(Long balanceGameId, User user);
}
