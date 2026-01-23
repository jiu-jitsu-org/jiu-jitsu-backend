package com.jiujitsu.api.domain.community.profile.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum SubmissionType {
    CHOKES("조르기"),
    ARM_LOCKS("팔 관절기"),
    LEG_LOCKS("다리 관절기");

    private final String description;
}
