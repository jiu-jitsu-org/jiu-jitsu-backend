package com.jiujitsu.api.domain.community.balance_game.mapper;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameOptionResponse;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class BalanceGameMapper {

    public BalanceGameResponse toResponse(BalanceGame game, long commentCount,
                                          long likeCount, boolean isLiked,
                                          boolean isCommented, Boolean noticeEnabled,
                                          long voteCountA, long voteCountB,
                                          BalanceGameOption myVote, LocalDateTime now) {
        return toResponse(game, commentCount, likeCount, isLiked, isCommented, noticeEnabled, voteCountA, voteCountB, myVote, now, null);
    }

    // resultUntil: 메인에서 마감된 게임을 결과로 보여 줄 때 그 끝 시각 (그 외 null)
    public BalanceGameResponse toResponse(BalanceGame game, long commentCount,
                                          long likeCount, boolean isLiked,
                                          boolean isCommented, Boolean noticeEnabled,
                                          long voteCountA, long voteCountB,
                                          BalanceGameOption myVote, LocalDateTime now, LocalDateTime resultUntil) {
        return new BalanceGameResponse(
                game.getContent().getId(),
                game.getGameDate(),
                game.getStartAt(),
                game.getEndAt(),
                now,                                    // serverTime (기기 시계 대체 기준점)
                game.isStarted(now),                    // started
                game.isClosed(now),                     // closed
                resultUntil,
                new BalanceGameOptionResponse(
                        BalanceGameOption.A, game.getOptionAText(), ImageInfo.from(game.getOptionAImage()), voteCountA),
                new BalanceGameOptionResponse(
                        BalanceGameOption.B, game.getOptionBText(), ImageInfo.from(game.getOptionBImage()), voteCountB),
                voteCountA + voteCountB,
                myVote,
                commentCount,
                likeCount,
                game.getContent().getViewCount(),
                isLiked,
                isCommented,
                noticeEnabled
        );
    }
}
