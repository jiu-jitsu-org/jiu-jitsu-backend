package com.jiujitsu.api.domain.user.dto;

import java.time.LocalDateTime;

public record AdminWithdrawalListItem(
        Long id,
        String nickname,
        LocalDateTime requestedAt,          // = deletedAt
        LocalDateTime deletionScheduledAt,  // = deletedAt + 30일
        WithdrawalStatus status
) {
    public enum WithdrawalStatus {
        PENDING,   // 유예 중
        DELETED    // 삭제 완료 (30일 경과)
    }
}
