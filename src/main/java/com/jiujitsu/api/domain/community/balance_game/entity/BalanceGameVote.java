package com.jiujitsu.api.domain.community.balance_game.entity;

import com.jiujitsu.api.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Table(
        name = "balance_game_vote",
        indexes = {
                @Index(name = "idx_balance_game_vote", columnList = "balance_game_id")
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

    // 다른 선택지로 투표 변경
    public void changeOption(BalanceGameOption option) {
        this.option = option;
    }
}
