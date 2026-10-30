package com.jiujitsu.api.domain.community.profile.entity;

/**
 * 체급 kg 기준 이하/초과 (예: 이하 76 → -76kg, 초과 100.5 → +100.5kg)
 */
public enum CompetitionWeightBound {
    UNDER,
    OVER
}
