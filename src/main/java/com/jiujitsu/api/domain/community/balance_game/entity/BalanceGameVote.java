package com.jiujitsu.api.domain.community.balance_game.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "balance_game_vote",
        indexes = {
                @Index(name = "idx_balance_game_vote", columnList = "balance_game_id"),
                // 내가 투표한 게임 목록 (유니크 제약은 balance_game_id 선두라 투표자 단독 조회에 못 탄다)
                @Index(name = "idx_balance_game_vote_created_by", columnList = "created_by")
        },
        // 1인 1표 보장 (투표자 = 감사 필드 created_by)
        uniqueConstraints = @UniqueConstraint(columnNames = {"balance_game_id", "created_by"})
)
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceGameVote extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "balance_game_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private BalanceGame balanceGame;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 1)
    private BalanceGameOption option;   // 선택한 선택지 (A/B)

    // 끝난 판의 결과를 처음 본 시각 (#290). 안 봤으면 null
    // 기록은 BalanceGameVoteRepository#markResultSeen 으로만 — 먼저 읽어 둔 투표 변경의 저장이 null 로 되돌리지 않게 updatable = false
    @Column(name = "result_seen_at", updatable = false)
    private LocalDateTime resultSeenAt;

    // 결과 알림을 보낸 시각 (#291). 안 보냈으면 null — 한 사람 한 판에 한 번만 보내게 하는 기록
    // 기록은 BalanceGameVoteRepository#markResultNoticed 로만 (resultSeenAt 과 같은 이유로 updatable = false)
    @Column(name = "result_noticed_at", updatable = false)
    private LocalDateTime resultNoticedAt;

    // 다른 선택지로 투표 변경
    public void changeOption(BalanceGameOption option) {
        this.option = option;
    }

    public boolean isResultSeen() {
        return resultSeenAt != null;
    }
}
