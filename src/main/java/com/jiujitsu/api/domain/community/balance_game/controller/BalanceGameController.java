package com.jiujitsu.api.domain.community.balance_game.controller;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.service.BalanceGameService;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "balance-game-controller", description = "커뮤니티 밸런스 게임 API")
@CommonApiResponses
@RestController
@RequestMapping("/community/balance-game")
@RequiredArgsConstructor
public class BalanceGameController {

    private final BalanceGameService balanceGameService;

    @Operation(
            summary = "오늘의 밸런스 게임 조회",
            description = "커뮤니티 메인에 노출할 현재 진행중인 밸런스 게임을 조회합니다. 진행중인 게임이 없으면 data 가 null 입니다."
    )
    @GetMapping
    public BalanceGameResponse getCurrent() {
        return balanceGameService.getCurrent();
    }
}
