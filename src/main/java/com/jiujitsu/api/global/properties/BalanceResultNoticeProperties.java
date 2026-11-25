package com.jiujitsu.api.global.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 밸런스 게임 결과 알림 기준 (#291). application.yml balance-game.result-notice 에서 바꾼다 (BalanceResultNoticeService 가 사용)
 * - minVotes: 총 투표가 이보다 적은 판은 보내지 않음
 * 보내는 시각(cron)은 BalanceResultNoticeRunService 가 직접 읽는다
 */
@ConfigurationProperties("balance-game.result-notice")
public record BalanceResultNoticeProperties(long minVotes) {

    public BalanceResultNoticeProperties {
        if (minVotes <= 0) {
            throw new IllegalArgumentException("balance-game.result-notice 의 min-votes 는 1 이상: " + minVotes);
        }
    }
}
