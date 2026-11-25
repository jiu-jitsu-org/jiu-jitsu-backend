package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.UserStatus;
import io.swagger.v3.oas.annotations.media.Schema;

public record AdminUserListRequest(
        @Schema(description = "닉네임 또는 이메일 검색어")
        String keyword,

        @Schema(description = "계정 상태 필터")
        UserStatus status,

        @Schema(description = "현재 제재 타입 필터 (생략 = 전체, NONE = 제재 없음, WARNING / SUSPENSION / PERMANENT_BAN)")
        String sanctionType,

        @Schema(description = "정렬 기준", allowableValues = {"createdAt", "reportCount", "violationCount"})
        String sort
) {}
