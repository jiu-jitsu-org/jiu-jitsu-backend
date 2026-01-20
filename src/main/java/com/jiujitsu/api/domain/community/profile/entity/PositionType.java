package com.jiujitsu.api.domain.community.profile.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum PositionType {
    TOP("탑 포지션"),
    GUARD("가드 포지션");

    private final String description;
}
