package com.jiujitsu.api.domain.community.balance_game.mapper;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameOptionResponse;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
import org.springframework.stereotype.Component;

@Component
public class BalanceGameMapper {

    public BalanceGameResponse toResponse(BalanceGame game, long commentCount) {
        return new BalanceGameResponse(
                game.getContent().getId(),
                game.getEndAt(),
                new BalanceGameOptionResponse(game.getOptionAText(), ImageInfo.from(game.getOptionAImage())),
                new BalanceGameOptionResponse(game.getOptionBText(), ImageInfo.from(game.getOptionBImage())),
                commentCount
        );
    }
}
