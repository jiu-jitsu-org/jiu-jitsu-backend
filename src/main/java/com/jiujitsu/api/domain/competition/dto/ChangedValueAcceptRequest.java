package com.jiujitsu.api.domain.competition.dto;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

/**
 * 바뀐 값 처리 — 크롤러 값으로 고치기 / 지금 값 유지 요청
 * fieldNames: 처리할 칸 이름 목록 (비면 전체 처리)
 */
public record ChangedValueAcceptRequest(
        @NotEmpty List<String> fieldNames
) {}
