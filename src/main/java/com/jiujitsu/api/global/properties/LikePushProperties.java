package com.jiujitsu.api.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * 좋아요 푸시 기준 (#196). application.yml notice.like-push 에서 바꾼다 (LikeNoticeService 가 사용)
 * - thresholds: 처음 기준들 (오름차순, 예: 1, 5, 10)
 * - step: 마지막 기준 뒤로 이 간격마다 (예: 10 → 20, 30, …)
 */
@ConfigurationProperties("notice.like-push")
public record LikePushProperties(List<Integer> thresholds, int step) {

    public LikePushProperties {
        if (thresholds == null || thresholds.isEmpty() || step <= 0) {
            throw new IllegalArgumentException("notice.like-push 설정이 비었거나 step 이 0 이하");
        }
        for (int i = 0; i < thresholds.size(); i++) {
            int t = thresholds.get(i);
            if (t <= 0 || (i > 0 && t <= thresholds.get(i - 1))) {
                throw new IllegalArgumentException("notice.like-push.thresholds 는 양수 오름차순: " + thresholds);
            }
        }
        thresholds = List.copyOf(thresholds);
    }

    /**
     * count 이하인 가장 큰 기준. 첫 기준보다 적으면 0
     */
    public int reachedThreshold(long count) {
        int last = thresholds.get(thresholds.size() - 1);
        if (count >= last) {
            return Math.toIntExact(last + (count - last) / step * step);
        }
        int reached = 0;
        for (int t : thresholds) {
            if (t > count) break;
            reached = t;
        }
        return reached;
    }
}
