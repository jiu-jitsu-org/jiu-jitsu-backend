package com.jiujitsu.api.domain.community.balance_game.controller;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameVoteRequest;
import com.jiujitsu.api.domain.community.balance_game.service.BalanceGameService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

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

    @Operation(
            summary = "내가 투표한 밸런스 게임 목록 조회",
            description = "내가 투표한 밸런스 게임 목록을 gameDate 최신순으로 조회합니다. 마감된 게임도 포함하며, "
                    + "투표를 취소한 게임은 제외됩니다. 항목은 상세 조회와 같은 형태이며 조회수는 증가하지 않습니다. "
                    + "정렬은 서버에서 고정하므로 sort 파라미터는 무시됩니다."
    )
    @LoginErrorExamples
    @GetMapping("/participated")
    public Page<BalanceGameResponse> participatedList(@PageableDefault(size = 20) Pageable pageable) {
        return balanceGameService.getParticipatedList(pageable);
    }

    @Operation(
            summary = "밸런스 게임 상세 조회",
            description = "밸런스 게임 상세 정보를 조회합니다. 마감된 게임도 조회 가능하며 결과만 노출됩니다. "
                    + "호출할 때마다 조회수(viewCount)가 1 증가합니다. (메인의 '오늘의 게임' 조회는 미증가) "
                    + "댓글은 별도로 댓글 목록 API(/community/comments)를 contentId 로 호출합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.BALANCE_GAME_NOT_FOUND})
    @GetMapping("/{contentId}")
    public BalanceGameResponse getDetail(
            @Parameter(name = "contentId", description = "컨텐츠 ID", required = true)
            @PathVariable(name = "contentId") Long contentId
    ) {
        return balanceGameService.getDetail(contentId);
    }

    @Operation(
            summary = "밸런스 게임 투표/취소",
            description = "밸런스 게임에 투표합니다. 이미 투표한 선택지를 다시 보내면 취소, 다른 선택지면 변경됩니다. "
                    + "투표가 반영된 최신 상태를 반환하므로 클라이언트는 별도 재조회가 필요 없습니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.BALANCE_GAME_NOT_FOUND, ErrorCode.BALANCE_GAME_CLOSED})
    @PostMapping("/{contentId}/vote")
    public BalanceGameResponse vote(
            @Parameter(name = "contentId", description = "컨텐츠 ID", required = true)
            @PathVariable(name = "contentId") Long contentId,
            @Valid @RequestBody BalanceGameVoteRequest request
    ) {
        return balanceGameService.vote(contentId, request.option());
    }
}
