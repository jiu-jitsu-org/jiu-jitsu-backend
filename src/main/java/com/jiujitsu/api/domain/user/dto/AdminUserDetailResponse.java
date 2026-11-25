package com.jiujitsu.api.domain.user.dto;

import com.jiujitsu.api.domain.user.entity.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record AdminUserDetailResponse(
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
        SanctionType currentSanctionType,
        LocalDateTime sanctionEndedAt,
        List<InterestItem> interests,
        List<CompetitionRecordItem> competitionRecords,
        List<SanctionHistoryItem> sanctionHistory
) {
    public record InterestItem(
            Long competitionId,
            String competitionName,
            LocalDate openDate,
            boolean noticeEnabled
    ) {}

    public record CompetitionRecordItem(
            Long recordId,
            Long competitionId,       // null = 직접 입력
            String competitionName,
            int year,
            boolean pinned
    ) {}

    public record SanctionHistoryItem(
            Long id,
            SanctionType type,
            SanctionReason reason,
            String reasonDetail,
            LocalDateTime startedAt,
            LocalDateTime endedAt,
            String processedByName,
            LocalDateTime releasedAt,
            String releasedByName,
            String releasedReason
    ) {}
}
