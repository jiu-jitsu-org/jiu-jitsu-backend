package com.jiujitsu.api.domain.competition.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;

@Schema(description = "조기 마감 반영 요청 — 적어도 하나의 마감일이 있어야 함")
public record EarlyCloseRequest(
        @Schema(description = "새 접수 마감일 (null이면 변경 안 함)", nullable = true)
        LocalDate registrationDeadline,

        @Schema(description = "새 얼리버드 마감일 (null이면 변경 안 함)", nullable = true)
        LocalDate earlyBirdDeadline
) {}
