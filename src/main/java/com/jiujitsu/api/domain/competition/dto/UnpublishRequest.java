package com.jiujitsu.api.domain.competition.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 비공개 전환 / 공개 안 함 요청 — 사유 필수
 */
public record UnpublishRequest(
        @NotBlank String reason
) {}
