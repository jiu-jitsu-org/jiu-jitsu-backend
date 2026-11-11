package com.jiujitsu.api.domain.community.balance_game.entity;

import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.global.entity.BaseEntity;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

@Entity
@Table(name = "balance_game")
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BalanceGame extends BaseEntity {
    // 게임 교체 뒤 결과를 보여 주는 시간 (00:00~00:30). 다음 게임의 기본 시작이 진행일 00:30 인 이유
    public static final Duration RESULT_BUFFER = Duration.ofMinutes(30);

    // 다음 게임이 이 안에 시작하면 "이어지는 다음 게임"으로 보고 그 시작까지 결과를 보여 준다 (며칠 뒤 게임까지 지난 결과를 붙잡지 않게)
    private static final Duration NEXT_GAME_WINDOW = Duration.ofDays(1);


    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 댓글/좋아요/조회수 등 커뮤니티 공통 기능은 Content 를 재사용한다. (contentType = BALANCE)
    @OneToOne(fetch = FetchType.LAZY, optional = false, cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "content_id", nullable = false, unique = true)
    private Content content;

    @Column(nullable = false, length = 100)
    private String optionAText;         // 선택지 A 문구

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_a_image_id")
    private ImageFile optionAImage;     // 선택지 A 이미지 (선택)

    @Column(nullable = false, length = 100)
    private String optionBText;         // 선택지 B 문구

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "option_b_image_id")
    private ImageFile optionBImage;     // 선택지 B 이미지 (선택)

    // 투표 시작 일시. 기본은 진행일 00:30 — 00:00~00:30 은 전날 게임 결과를 보여 주는 버퍼 (#195)
    // 등록 때 정해 저장한다 (버퍼 정책이 바뀌어도 지난 게임 일정은 그대로)
    @Column(name = "start_at", nullable = false)
    private LocalDateTime startAt;

    @Column(nullable = false)
    private LocalDateTime endAt;        // 마감 일시 (프론트 카운트다운 기준)

    // 게임이 "진행된" 날짜. 등록 시각(createdAt)·마감 일시(endAt)와 의미가 달라 별도로 보관한다.
    // - createdAt: 심야 등록 시 진행일 전날이 된다 (BalanceGameService.resolveEndAt 참고)
    // - endAt: 마감 정책이 바뀌면(예: 익일 새벽 마감) 날짜가 밀려 과거 데이터까지 어긋난다
    @Column(name = "game_date", nullable = false)
    private LocalDate gameDate;

    // 기본 시작 = 진행일 00:00 + 버퍼
    public static LocalDateTime defaultStartAt(LocalDate gameDate) {
        return gameDate.atStartOfDay().plus(RESULT_BUFFER);
    }

    // 투표를 받기 시작했는지 (시작 시각 포함)
    public boolean isStarted(LocalDateTime now) {
        return !startAt.isAfter(now);
    }

    // 마감됐는지 (마감 시각까지는 진행 중 — 기존 판정 그대로)
    public boolean isClosed(LocalDateTime now) {
        return endAt.isBefore(now);
    }

    // 투표할 수 있는지: 시작 전이면 BALANCE_GAME_NOT_STARTED, 마감 뒤면 BALANCE_GAME_CLOSED
    public void validateVotable(LocalDateTime now) {
        if (!isStarted(now)) {
            throw new ErrorException(ErrorCode.BALANCE_GAME_NOT_STARTED);
        }
        if (isClosed(now)) {
            throw new ErrorException(ErrorCode.BALANCE_GAME_CLOSED);
        }
    }

    /**
     * 마감 뒤 메인에 이 게임 결과를 보여 주는 끝 시각 (이 시각부터 메인에서 빠진다)
     * - 이어지는 다음 게임(마감 뒤 하루 안에 시작)이 있으면 그 시작 시각 — 결과 노출과 다음 게임 시작 사이에 빈틈·겹침이 없게
     * - 없으면 마감(분 단위로 올림) + 버퍼. 기본 마감 23:59:59 는 다음 날 00:30:00(다음 게임 기본 시작)에 맞고,
     *   22:00:00 처럼 직접 넣은 마감은 22:30:00 그대로
     */
    public LocalDateTime resultUntil(BalanceGame nextGame) {
        if (nextGame != null && nextGame.getStartAt().isAfter(endAt)
                && !nextGame.getStartAt().isAfter(endAt.plus(NEXT_GAME_WINDOW))) {
            return nextGame.getStartAt();
        }
        LocalDateTime closedMinute = endAt.truncatedTo(ChronoUnit.MINUTES);
        if (closedMinute.isBefore(endAt)) {
            closedMinute = closedMinute.plusMinutes(1);
        }
        return closedMinute.plus(RESULT_BUFFER);
    }
}
