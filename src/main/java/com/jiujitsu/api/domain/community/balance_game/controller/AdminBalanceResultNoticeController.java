package com.jiujitsu.api.domain.community.balance_game.controller;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceResultNoticeRunResponse;
import com.jiujitsu.api.domain.community.balance_game.service.BalanceResultNoticeRunService;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "admin-balance-result-notice-controller", description = "관리자 밸런스 게임 결과 알림 API")
@CommonApiResponses
@AdminOnly
@RestController
@RequestMapping("/admin/balance-game/result-notices")
@RequiredArgsConstructor
public class AdminBalanceResultNoticeController {

    private final BalanceResultNoticeRunService balanceResultNoticeRunService;

    @Operation(
            summary = "밸런스 게임 결과 알림 지금 보내기",
            description = "매일 낮(기본 11:55) 자동으로 도는 결과 알림(실행일 전날 마감된 판)을 지금 돌립니다. (관리자·운영자 토큰 필요)\n\n"
                    + "- 그 시각에 서버가 내려가 있었거나 실패한 날, **같은 날 자정 전에** 부릅니다. 자정이 지나면 놓친 판은 보내지 않고, 대신 방금 끝난 판의 알림을 지금(한밤중) 보냅니다\n"
                    + "- 이미 받은 사람·결과를 본 사람은 건너뛰므로 여러 번 불러도 같은 알림이 다시 가지 않습니다\n"
                    + "- 응답 closedDate 로 어느 날 마감된 판을 보냈는지 확인합니다"
    )
    @PostMapping
    public BalanceResultNoticeRunResponse runYesterday() {
        return balanceResultNoticeRunService.runYesterday();
    }
}
