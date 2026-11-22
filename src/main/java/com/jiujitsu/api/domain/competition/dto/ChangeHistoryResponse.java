package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.ChangeActorType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

@Schema(description = "변경 이력 한 건")
public record ChangeHistoryResponse(
        @Schema(description = "이력 ID") Long id,
        @Schema(description = "변경 시각") LocalDateTime createdAt,
        @Schema(description = "행위자 종류 (OPERATOR · CRAWLER · SERVER)") ChangeActorType actorType,
        @Schema(description = "행위자 이름 (운영자 계정 이름 또는 '크롤러' · '서버')", example = "김운영") String actorName,
        @Schema(description = "동작", example = "field_save") String action,
        @Schema(description = "칸 이름 (없으면 null)", nullable = true) String fieldName,
        @Schema(description = "이전 값 (없으면 null)", nullable = true) String oldValue,
        @Schema(description = "새 값 (없으면 null)", nullable = true) String newValue,
        @Schema(description = "사유 (없으면 null)", nullable = true) String reason,
        @Schema(description = "메모 (없으면 null)", nullable = true) String memo
) {}
