package com.jiujitsu.api.domain.community.balance_game.dto;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Schema(description = "관리자 밸런스 게임 수정 요청")
public record AdminBalanceGameUpdateRequest(
        @Size(max = BalanceGame.TITLE_MAX_LENGTH, message = "타이틀은 30자 이하여야 합니다.")
        @Schema(description = "타이틀 (최대 30자, null이면 제거)", example = "더 싫은 패배는?", nullable = true)
        String title,

        @NotBlank(message = "선택지 A 문구는 필수입니다.")
        @Size(max = BalanceGame.OPTION_TEXT_MAX_LENGTH, message = "선택지 A 문구는 100자 이하여야 합니다.")
        @Schema(description = "선택지 A 문구", example = "가드 패스 성공률 100%지만 서브 미션 절대 안 됨", requiredMode = Schema.RequiredMode.REQUIRED)
        String optionAText,

        @Schema(description = "선택지 A 이미지 파일 ID (null이면 이미지 제거)", example = "1")
        Long optionAImageFileId,

        @NotBlank(message = "선택지 B 문구는 필수입니다.")
        @Schema(description = "선택지 B 문구", example = "서브 미션 성공률 100%지만 가드 패스 절대 못 함", requiredMode = Schema.RequiredMode.REQUIRED)
        String optionBText,

        @Schema(description = "선택지 B 이미지 파일 ID (null이면 이미지 제거)", example = "2")
        Long optionBImageFileId,

        @Schema(description = "투표 시작 일시 (미입력 시 기존 값 유지)", example = "2026-11-26T00:30:00")
        LocalDateTime startAt,

        @Schema(description = "마감 일시 (미입력 시 기존 값 유지)", example = "2026-11-26T23:59:59")
        LocalDateTime endAt
) {}
