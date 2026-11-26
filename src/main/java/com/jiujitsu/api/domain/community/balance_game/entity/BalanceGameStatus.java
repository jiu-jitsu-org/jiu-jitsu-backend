package com.jiujitsu.api.domain.community.balance_game.entity;

public enum BalanceGameStatus {
    UPCOMING,  // 시작 전 (startAt > now)
    ONGOING,   // 진행 중 (startAt <= now && endAt >= now)
    CLOSED     // 마감 (endAt < now)
}
