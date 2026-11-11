package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.notice.dto.CompetitionDeadlineNoticeRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 관심 대회 마감 전날 알림 — 매일 저녁(기본 18시, competition.deadline-notice.cron) 내일 끝나는 마감을 보낸다
 * 보내는 시각에만 돌기 때문에 그 뒤에 관심 등록한 대회의 내일 마감은 보내지 않는다 (지난 마감도 보내지 않음)
 * 그 시각에 서버가 내려가 있었거나 실패했으면 관리자가 같은 작업을 다시 돌린다 (AdminCompetitionDeadlineNoticeController)
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompetitionDeadlineNoticeScheduler {

    private final CompetitionDeadlineNoticeService competitionDeadlineNoticeService;
    private final Clock clock;

    @Scheduled(cron = "${competition.deadline-notice.cron}", zone = "Asia/Seoul")
    public void scheduled() {
        sendTomorrowDeadlines();
    }

    /**
     * 내일 끝나는 마감 알림을 지금 보낸다. 이미 받은 유저는 발송 기록으로 건너뛰어 여러 번 돌려도 같은 알림이 다시 가지 않는다
     * 마감 하나마다 send 가 따로 커밋하므로(다른 빈 프록시 경유) 하나가 실패해도 나머지는 보낸다
     */
    public CompetitionDeadlineNoticeRunResponse sendTomorrowDeadlines() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        int deadlines = 0;
        int failed = 0;
        int sent = 0;
        for (CompetitionDeadlineNoticeService.DueNotice due : competitionDeadlineNoticeService.findDue(tomorrow)) {
            deadlines++;
            try {
                sent += competitionDeadlineNoticeService.send(due);
            } catch (RuntimeException e) {
                // 실패한 마감은 롤백된다 (발송 기록도 없음) — 같은 날 관리자 재실행으로 다시 보낼 수 있다
                failed++;
                log.warn("대회 마감 알림 실패 competitionId={} type={} deadline={}",
                        due.competitionId(), due.deadline().type(), due.deadline().date(), e);
            }
        }
        log.info("대회 마감 알림 마감일 {} — 마감 {}개(실패 {}), 보낸 알림 {}건", tomorrow, deadlines, failed, sent);
        return new CompetitionDeadlineNoticeRunResponse(tomorrow, deadlines, failed, sent);
    }
}
