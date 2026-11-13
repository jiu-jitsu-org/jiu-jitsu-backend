package com.jiujitsu.api.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 관심 많은 대회 표시 기준 (#210). application.yml competition.popular 에서 바꾼다 (CompetitionQueryService 가 사용)
 * - top: 같은 달 끝나지 않은 공개 대회 중 관심 수 상위 몇 개
 * - minInterests: 관심 수가 이보다 적으면 상위여도 표시하지 않음 (오픈 초기 관심 1~2개 대회에 붙지 않게)
 */
@ConfigurationProperties("competition.popular")
public record PopularCompetitionProperties(int top, long minInterests) {

    public PopularCompetitionProperties {
        if (top <= 0 || minInterests <= 0) {
            throw new IllegalArgumentException("competition.popular 의 top·min-interests 는 1 이상: top=" + top + ", minInterests=" + minInterests);
        }
    }
}
