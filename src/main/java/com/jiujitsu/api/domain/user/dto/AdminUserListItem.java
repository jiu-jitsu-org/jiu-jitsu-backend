package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.*;

import java.time.LocalDateTime;

public record AdminUserListItem(
        Long id,
        String nickname,
        String email,
        SnsProvider snsProvider,
        AccountType accountType,
        UserRole role,
        UserStatus status,
        LocalDateTime createdAt,
        LocalDateTime deletedAt,
        int reportCount,
        int violationCount,
        SanctionType currentSanctionType,   // null = 제재 없음
        LocalDateTime sanctionEndedAt       // 일시정지 종료일 (null = 경고·영구·없음)
) {}
