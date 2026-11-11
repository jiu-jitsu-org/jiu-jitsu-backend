package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.notice.dto.CompetitionDeadlineNoticeRunResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDate;

/**
 * 관심 대회 마감 전날 알림 실행 — 매일 저녁(기본 18시, competition.deadline-notice.cron) 내일 끝나는 마감을 보낸다
 * 보내는 시각에만 돌기 때문에 그 뒤에 관심 등록한 대회의 내일 마감은 보내지 않는다 (지난 마감도 보내지 않음)
 * 그 시각에 서버가 내려가 있었거나 실패했으면 관리자가 같은 날 자정 전에 다시 돌린다 (AdminCompetitionDeadlineNoticeController)
 * 마감별 발송(CompetitionDeadlineNoticeService#send, REQUIRES_NEW)은 다른 빈이라 프록시를 거쳐 마감마다 따로 커밋된다
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CompetitionDeadlineNoticeRunService {

    private final CompetitionDeadlineNoticeService competitionDeadlineNoticeService;
    private final Clock clock;

    @Scheduled(cron = "${competition.deadline-notice.cron}", zone = "Asia/Seoul")
    public void runDaily() {
        runTomorrow();
    }

    /**
     * 내일(서울 기준 실행일 다음 날) 끝나는 마감 알림을 지금 보낸다. 이미 받은 유저는 발송 기록으로 건너뛴다
     * 하나가 실패해도 나머지는 보낸다. 다른 실행(정기·관리자)과 겹쳐 같은 기록이 먼저 들어가 있으면 그 마감은 이번 실행에서 건너뛴다
     */
    public CompetitionDeadlineNoticeRunResponse runTomorrow() {
        LocalDate tomorrow = LocalDate.now(clock).plusDays(1);
        int deadlines = 0;
        int overlapped = 0;
        int failed = 0;
        int sent = 0;
        for (CompetitionDeadlineNoticeService.DueNotice due : competitionDeadlineNoticeService.findDue(tomorrow)) {
            deadlines++;
            try {
                sent += competitionDeadlineNoticeService.send(due);
            } catch (DataIntegrityViolationException e) {
                // 발송 기록 유니크 충돌 = 동시에 돈 다른 실행이 먼저 보냄. 그 사이 새로 대상이 된 유저는 다시 실행하면 받는다
                overlapped++;
                log.info("대회 마감 알림 다른 실행과 겹침 competitionId={} type={} deadline={}",
                        due.competitionId(), due.deadline().type(), due.deadline().date());
            } catch (RuntimeException e) {
                // 실패한 마감은 롤백된다 (발송 기록도 없음) — 같은 날 자정 전 관리자 재실행으로 다시 보낼 수 있다
                failed++;
                log.warn("대회 마감 알림 실패 competitionId={} type={} deadline={}",
                        due.competitionId(), due.deadline().type(), due.deadline().date(), e);
            }
        }
        log.info("대회 마감 알림 마감일 {} — 마감 {}개(겹침 {}, 실패 {}), 보낸 알림 {}건", tomorrow, deadlines, overlapped, failed, sent);
        return new CompetitionDeadlineNoticeRunResponse(tomorrow, deadlines, overlapped, failed, sent);
    }
}
