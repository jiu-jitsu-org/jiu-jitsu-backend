package com.jiujitsu.api.domain.notice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 관심 대회 마감 전날 알림 — 매일 저녁(기본 18시, competition.deadline-notice.cron) 내일 끝나는 마감을 보낸다
 * 보내는 시각에만 돌기 때문에 그 뒤에 관심 등록한 대회의 내일 마감은 보내지 않는다 (지난 마감도 보내지 않음)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionDeadlineNoticeScheduler {

    private final CompetitionDeadlineNoticeService competitionDeadlineNoticeService;
    private final Clock clock;

    @Scheduled(cron = "${competition.deadline-notice.cron}", zone = "Asia/Seoul")
    public void sendTomorrowDeadlines() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        int sent = 0;
        for (CompetitionDeadlineNoticeService.DueNotice due : competitionDeadlineNoticeService.findDue(tomorrow)) {
            try {
                sent += competitionDeadlineNoticeService.send(due);
            } catch (RuntimeException e) {
                // 한 마감이 실패해도 나머지는 보낸다 (그 마감은 롤백돼 다음 실행 때 다시 시도되지 않음 — 다음 날엔 마감이 내일이 아님)
                log.warn("대회 마감 알림 실패 competitionId={} type={} deadline={}",
                        due.competitionId(), due.deadline().type(), due.deadline().date(), e);
            }
        }
        log.info("대회 마감 알림 {} 건 (마감일 {})", sent, tomorrow);
    }
}
