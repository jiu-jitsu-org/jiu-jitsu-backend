package com.jiujitsu.api.domain.community.balance_game.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "밸런스 게임 결과 알림 실행 결과")
public record BalanceResultNoticeRunResponse(
        @Schema(description = "대상 판의 마감 날짜 (실행일 전날, 서울 기준)", example = "2026-11-25") LocalDate closedDate,
        @Schema(description = "대상 판 수 (그날 마감됐고 총 투표가 기준 이상인 판. 모두 이미 보냈어도 셈)", example = "1") int games,
        @Schema(description = "이번 실행에서 보낸 알림 수 (이미 받았거나 결과를 본 사람은 빠짐)", example = "12") int sent,
        @Schema(description = "실패한 판 수 (서버 로그 확인, 다시 부르면 재시도)", example = "0") int failed
) {
}
