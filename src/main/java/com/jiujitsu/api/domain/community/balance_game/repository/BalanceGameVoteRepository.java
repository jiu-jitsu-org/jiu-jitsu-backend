package com.jiujitsu.api.domain.community.balance_game.repository;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BalanceGameVoteRepository extends JpaRepository<BalanceGameVote, Long> {

    // 선택지별 득표수
    long countByBalanceGameIdAndOption(Long balanceGameId, BalanceGameOption option);

    // 현재 사용자의 투표 (myVote / 토글 판단)
    Optional<BalanceGameVote> findByBalanceGameIdAndCreatedBy(Long balanceGameId, User user);

    /**
     * 내가 투표한 밸런스 게임 목록 (마감 포함, gameDate 최신순)
     * 투표 취소는 행 삭제라 별도 조건 없이 제외된다.
     * 정렬은 쿼리에 고정하므로 Pageable 은 정렬 없이 넘겨야 한다.
     */
    @Query(value = """
            SELECT v
            FROM BalanceGameVote v
            JOIN FETCH v.balanceGame g
            JOIN FETCH g.content
            LEFT JOIN FETCH g.optionAImage
            LEFT JOIN FETCH g.optionBImage
            WHERE v.createdBy = :user
            ORDER BY g.gameDate DESC, g.id DESC
            """,
            countQuery = """
            SELECT COUNT(v)
            FROM BalanceGameVote v
            WHERE v.createdBy = :user
            """)
    Page<BalanceGameVote> findMyVotes(@Param("user") User user, Pageable pageable);

    /**
     * 게임 다건의 선택지별 득표수 [balanceGameId, option, count]
     */
    @Query("""
            SELECT v.balanceGame.id, v.option, COUNT(v)
            FROM BalanceGameVote v
            WHERE v.balanceGame.id IN :balanceGameIds
            GROUP BY v.balanceGame.id, v.option
            """)
    List<Object[]> countByBalanceGameIdsGroupByOption(@Param("balanceGameIds") List<Long> balanceGameIds);
}
