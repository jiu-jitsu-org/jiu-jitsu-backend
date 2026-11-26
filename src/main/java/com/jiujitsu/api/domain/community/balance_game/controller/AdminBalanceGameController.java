package com.jiujitsu.api.domain.community.balance_game.controller;

import com.jiujitsu.api.domain.community.balance_game.dto.AdminBalanceGameListItem;
import com.jiujitsu.api.domain.community.balance_game.dto.AdminBalanceGameListRequest;
import com.jiujitsu.api.domain.community.balance_game.dto.AdminBalanceGameStatsResponse;
import com.jiujitsu.api.domain.community.balance_game.dto.AdminBalanceGameUpdateRequest;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameCreateRequest;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.service.BalanceGameService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.security.annotation.AdminOnly;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

@Tag(name = "admin-balance-game-controller", description = "관리자 밸런스 게임 API")
@CommonApiResponses
@AdminOnly
@RestController
@RequestMapping("/admin/balance-game")
@RequiredArgsConstructor
public class AdminBalanceGameController {

    private final BalanceGameService balanceGameService;

    // ── 등록 ──────────────────────────────────────────────────────

    @Operation(
            summary = "밸런스 게임 등록",
            description = "새 밸런스 게임을 등록합니다. endAt 미입력 시 한국 시간 23:59:59 자동 설정."
    )
    @ApiErrorCodeExamples({ErrorCode.IMAGE_FILE_NOT_FOUND, ErrorCode.BALANCE_GAME_END_AT_MUST_BE_FUTURE, ErrorCode.BALANCE_GAME_START_AT_NOT_BEFORE_END_AT})
    @PostMapping
    public BalanceGameResponse create(@Valid @RequestBody BalanceGameCreateRequest request) {
        return balanceGameService.create(request);
    }

    // ── 목록 ──────────────────────────────────────────────────────

    @Operation(
            summary = "밸런스 게임 목록",
            description = "날짜 범위(dateFrom~dateTo)·상태(UPCOMING/ONGOING/CLOSED) 필터. gameDate 최신순. 20개씩."
    )
    @GetMapping
    public Page<AdminBalanceGameListItem> getList(
            @ModelAttribute AdminBalanceGameListRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        return balanceGameService.getAdminList(request, page, size);
    }

    // ── 상세 ──────────────────────────────────────────────────────

    @Operation(summary = "밸런스 게임 상세", description = "밸런스 게임 ID로 상세 정보를 조회합니다.")
    @ApiErrorCodeExamples({ErrorCode.BALANCE_GAME_NOT_FOUND})
    @GetMapping("/{id}")
    public BalanceGameResponse getDetail(
            @Parameter(description = "밸런스 게임 ID") @PathVariable Long id
    ) {
        return balanceGameService.getAdminDetail(id);
    }

    // ── 수정 ──────────────────────────────────────────────────────

    @Operation(
            summary = "밸런스 게임 수정",
            description = "선택지 문구·이미지·시작/마감 일시를 수정합니다. startAt/endAt 미입력 시 기존 값 유지. 이미지 파일 ID null이면 이미지 제거."
    )
    @ApiErrorCodeExamples({ErrorCode.BALANCE_GAME_NOT_FOUND, ErrorCode.IMAGE_FILE_NOT_FOUND, ErrorCode.BALANCE_GAME_START_AT_NOT_BEFORE_END_AT})
    @PutMapping("/{id}")
    public BalanceGameResponse update(
            @Parameter(description = "밸런스 게임 ID") @PathVariable Long id,
            @Valid @RequestBody AdminBalanceGameUpdateRequest request
    ) {
        return balanceGameService.updateAdmin(id, request);
    }

    // ── 삭제 ──────────────────────────────────────────────────────

    @Operation(
            summary = "밸런스 게임 삭제",
            description = "밸런스 게임과 연결된 투표 기록, 댓글, 좋아요, 조회수를 함께 삭제합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.BALANCE_GAME_NOT_FOUND})
    @DeleteMapping("/{id}")
    public void delete(
            @Parameter(description = "밸런스 게임 ID") @PathVariable Long id
    ) {
        balanceGameService.deleteAdmin(id);
    }

    // ── 결과 분석 ─────────────────────────────────────────────────

    @Operation(
            summary = "밸런스 게임 결과 분석",
            description = "참여 수, 선택지별 득표 수·득표율, 댓글 수, 조회 수를 반환합니다."
    )
    @ApiErrorCodeExamples({ErrorCode.BALANCE_GAME_NOT_FOUND})
    @GetMapping("/{id}/stats")
    public AdminBalanceGameStatsResponse getStats(
            @Parameter(description = "밸런스 게임 ID") @PathVariable Long id
    ) {
        return balanceGameService.getAdminStats(id);
    }
}
