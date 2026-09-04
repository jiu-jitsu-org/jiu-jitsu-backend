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
                                          long voteCountA, long voteCountB,
                                          BalanceGameOption myVote, LocalDateTime now) {
        return new BalanceGameResponse(
                game.getContent().getId(),
                game.getEndAt(),
                now,                                    // serverTime (기기 시계 대체 기준점)
                game.getEndAt().isBefore(now),          // closed
                new BalanceGameOptionResponse(
                        BalanceGameOption.A, game.getOptionAText(), ImageInfo.from(game.getOptionAImage()), voteCountA),
                new BalanceGameOptionResponse(
                        BalanceGameOption.B, game.getOptionBText(), ImageInfo.from(game.getOptionBImage()), voteCountB),
                voteCountA + voteCountB,
                myVote,
                commentCount,
                likeCount,
                isLiked
        );
    }
}
