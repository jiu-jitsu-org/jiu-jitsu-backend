package com.jiujitsu.api.domain.community.balance_game.repository;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface BalanceGameVoteRepository extends JpaRepository<BalanceGameVote, Long> {

    // 선택지별 득표수
    long countByBalanceGameIdAndOption(Long balanceGameId, BalanceGameOption option);

    // 현재 사용자의 투표 (myVote / 토글 판단)
    Optional<BalanceGameVote> findByBalanceGameIdAndCreatedBy(Long balanceGameId, User user);

    /**
     * 내 투표에 결과를 처음 본 시각을 남긴다 (#290)
     * 아직 안 본 행만 고쳐, 다시 · 동시에 불러도 처음 시각이 남는다. 투표하지 않았거나 이미 봤으면 0행.
     */
    @Modifying
    @Query("""
            UPDATE BalanceGameVote v
            SET v.resultSeenAt = :seenAt
            WHERE v.balanceGame.id = :balanceGameId
            AND v.createdBy = :user
            AND v.resultSeenAt IS NULL
            """)
    int markResultSeen(@Param("balanceGameId") Long balanceGameId,
                       @Param("user") User user,
                       @Param("seenAt") LocalDateTime seenAt);

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
