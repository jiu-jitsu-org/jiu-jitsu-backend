package com.jiujitsu.api.domain.community.balance_game.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

@Schema(description = "[임시] 밸런스 게임 등록 요청 (테스트 데이터용)")
public record BalanceGameCreateRequest(
        @NotBlank(message = "선택지 A 문구는 필수입니다.")
        @Schema(description = "선택지 A 문구", example = "가드 패스 성공률 100%지만 서브 미션 절대 안 됨", requiredMode = Schema.RequiredMode.REQUIRED)
            String optionAText,
        @Schema(description = "선택지 A 이미지 파일 ID (선택)", example = "1")
            Long optionAImageFileId,
        @NotBlank(message = "선택지 B 문구는 필수입니다.")
        @Schema(description = "선택지 B 문구", example = "서브 미션 성공률 100%지만 가드 패스 절대 못 함", requiredMode = Schema.RequiredMode.REQUIRED)
            String optionBText,
        @Schema(description = "선택지 B 이미지 파일 ID (선택)", example = "2")
            Long optionBImageFileId,
        @Schema(description = "마감 일시 (미입력 시 한국 시간 23:59:59 자동 설정 — startAt 을 넣었으면 시작일, 아니면 당일이고 이미 지났으면 다음 날)", example = "2026-07-15T23:59:59", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            LocalDateTime endAt,
        @Schema(description = "투표 시작 일시 (미입력 시 마감일 00:30 — 00:00~00:30 은 전날 결과 버퍼라 그 전엔 메인에 안 나오고 투표 불가). "
                + "마감보다 앞이어야 함(아니면 C0012). 날짜를 넘기는 게임은 시작을 함께 넣고, 진행일은 시작일", example = "2026-07-15T00:30:00", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            LocalDateTime startAt
) { }
