package com.jiujitsu.api.domain.community.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProfileRequestType {
    ACADEMY("도장정보(도장명)"),
    BELT_WEIGHT("벨트/성별/체급"),
    POSITION("포지션"),
    SUBMISSION("서브미션"),
    TECHNIQUE("기술"),
    COMPETITION("대회정보"),
    OWNER_INFO("관장/사범 지도정보");

    private final String description;
}
