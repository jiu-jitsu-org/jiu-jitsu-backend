package com.jiujitsu.api.domain.community.profile.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProfileRequestType {
    ACADEMY("도장정보(도장명)"),
    BELT_WEIGHT("벨트/성별/체급"),
    POSITION_BEST("포지션(특기)"),
    POSITION_FAVORITE("포지션(최애)"),
    SUBMISSION_BEST("서브미션(특기)"),
    SUBMISSION_FAVORITE("서브미션(최애)"),
    TECHNIQUE_BEST("기술(특기)"),
    TECHNIQUE_FAVORITE("기술(최애)"),
    COMPETITION("대회정보"),
    OWNER_INFO("관장/사범 지도정보");

    private final String description;
}
