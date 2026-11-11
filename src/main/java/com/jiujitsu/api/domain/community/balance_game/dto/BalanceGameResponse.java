package com.jiujitsu.api.domain.community.balance_game.dto;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Schema(description = "밸런스 게임 응답")
public record BalanceGameResponse(
        @Schema(description = "컨텐츠 ID (댓글 조회/상세·투표 이동용)", example = "1") Long contentId,
        @Schema(description = "게임 진행 날짜 (메타 행 표기용 - 밸런스 게임은 timeAgo 미노출)", example = "2026-09-10") LocalDate gameDate,
        @Schema(description = "투표 시작 일시 (보통 진행일 00:30)") LocalDateTime startAt,
        @Schema(description = "마감 일시 (프론트 카운트다운 기준)") LocalDateTime endAt,
        @Schema(description = "서버 응답 시각 (기기 시계 대신 이 값을 기준으로 남은 시간 계산)") LocalDateTime serverTime,
        @Schema(description = "투표 시작 여부 (false면 아직 시작 전, 투표 불가 — 서버 시각 기준)", example = "true") boolean started,
        @Schema(description = "마감 여부 (true면 결과만 노출, 투표 불가)", example = "false") boolean closed,
        @Schema(description = "메인(오늘의 게임)에서 마감된 게임을 결과로 보여 줄 때, 결과를 보여 주는 끝 시각 — 이 시각(serverTime 기준)에 메인을 다시 조회하면 다음 게임. 진행 중 게임·상세·목록에선 null", nullable = true) LocalDateTime resultUntil,
        @Schema(description = "선택지 A") BalanceGameOptionResponse optionA,
        @Schema(description = "선택지 B") BalanceGameOptionResponse optionB,
        @Schema(description = "전체 득표 수", example = "10") long totalVoteCount,
        @Schema(description = "내가 투표한 선택지 (null이면 미투표 → 결과·퍼센트 숨김)", example = "A") BalanceGameOption myVote,
        @Schema(description = "댓글 수 (0이면 '첫 댓글 남기러 가기'로 노출)", example = "5") long commentCount,
        @Schema(description = "좋아요 수", example = "17") long likeCount,
        @Schema(description = "조회 수 (상세 조회 시마다 증가)", example = "100") long viewCount,
        @Schema(description = "좋아요 여부 (비로그인 시 false)", example = "true") boolean isLiked,
        @Schema(description = "댓글 작성 여부 (비로그인 시 false)", example = "true") boolean isCommented,
        @Schema(description = "게시물 알림 활성화 여부 (비로그인 시 null)", example = "true") Boolean noticeEnabled
) { }
