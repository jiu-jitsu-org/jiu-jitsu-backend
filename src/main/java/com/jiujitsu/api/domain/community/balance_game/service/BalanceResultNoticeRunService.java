package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceResultNoticeRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 밸런스 게임 결과 알림 실행 (#291) — 매일 낮(기본 11:55, balance-game.result-notice.cron) 전날 마감된 판을 보낸다
 * 그 시각에 서버가 내려가 있었거나 실패했으면 관리자가 같은 날 자정 전에 다시 돌린다 (AdminBalanceResultNoticeController). 날이 지난 판은 보내지 않는다
 * 판별 발송(BalanceResultNoticeService#send, REQUIRES_NEW)은 다른 빈이라 프록시를 거쳐 판마다 따로 커밋된다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceResultNoticeRunService {

    private final BalanceResultNoticeService balanceResultNoticeService;
    private final Clock clock;

    @Scheduled(cron = "${balance-game.result-notice.cron}", zone = "Asia/Seoul")
    public void runDaily() {
        runYesterday();
    }

    /**
     * 어제(서울 기준 실행일 전날) 마감된 판의 결과 알림을 지금 보낸다. 이미 받은 사람은 건너뛴다
     * 판 하나가 실패하면 그 판만 롤백되고(보낸 기록도 없음) 나머지는 보낸다
     */
    public BalanceResultNoticeRunResponse runYesterday() {
        LocalDate yesterday = LocalDate.now(clock).minusDays(1);
        int games = 0;
        int sent = 0;
        int failed = 0;
        for (BalanceResultNoticeService.DueNotice due : balanceResultNoticeService.findDue(yesterday)) {
            games++;
            try {
                sent += balanceResultNoticeService.send(due);
            } catch (RuntimeException e) {
                failed++;
                log.warn("밸런스 게임 결과 알림 실패 contentId={}", due.contentId(), e);
            }
        }
        log.info("밸런스 게임 결과 알림 마감일 {} — 판 {}개(실패 {}), 보낸 알림 {}건", yesterday, games, failed, sent);
        return new BalanceResultNoticeRunResponse(yesterday, games, sent, failed);
    }
}
