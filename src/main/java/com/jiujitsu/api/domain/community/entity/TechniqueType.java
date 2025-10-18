package com.jiujitsu.api.domain.community.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum TechniqueType {
    SWEEPS("스윕&뒤집기"),
    GUARD_PASSES("가드 패스"),
    TAKE_DOWNS("테이크다운"),
    ESCAPES("탈출&방어");

    private final String description;
}
