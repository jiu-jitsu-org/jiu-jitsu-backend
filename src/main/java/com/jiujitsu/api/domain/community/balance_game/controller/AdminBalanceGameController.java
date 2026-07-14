package com.jiujitsu.api.domain.community.balance_game.controller;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameCreateRequest;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.service.BalanceGameService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-balance-game-controller", description = "[임시] 관리자 밸런스 게임 등록 API (테스트 데이터용)")
@CommonApiResponses
@RestController
@RequestMapping("/admin/balance-game")
@RequiredArgsConstructor
public class AdminBalanceGameController {

    private final BalanceGameService balanceGameService;

    @Operation(
            summary = "[임시] 밸런스 게임 등록",
            description = "테스트 데이터 생성을 위한 임시 관리자 등록 API 입니다. 추후 정식 관리자 기능으로 대체됩니다. (ROLE_ADMIN 필요)"
    )
    @ApiErrorCodeExamples({ErrorCode.IMAGE_FILE_NOT_FOUND})
    @PostMapping
    public BalanceGameResponse create(@Valid @RequestBody BalanceGameCreateRequest request) {
        return balanceGameService.create(request);
    }
}
