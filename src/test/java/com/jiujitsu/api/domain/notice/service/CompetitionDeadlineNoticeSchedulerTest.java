package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.notice.dto.CompetitionDeadlineNoticeRunResponse;
import com.jiujitsu.api.domain.notice.entity.CompetitionDeadlineType;
import com.jiujitsu.api.domain.notice.service.CompetitionDeadlineNoticeService.DueNotice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CompetitionDeadlineNoticeSchedulerTest {

    // 서울 2026-11-19 18:00 (UTC 09:00)
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-19T09:00:00Z"), ZoneId.of("Asia/Seoul"));
    private final CompetitionDeadlineNoticeService service = mock(CompetitionDeadlineNoticeService.class);
    private final CompetitionDeadlineNoticeScheduler scheduler = new CompetitionDeadlineNoticeScheduler(service, clock);

    private static DueNotice due(long competitionId) {
        LocalDate tomorrow = LocalDate.of(2026, 11, 20);
        return new DueNotice(competitionId, "대회" + competitionId,
                new CompetitionDeadline(CompetitionDeadlineType.REGISTRATION, "접수", tomorrow));
    }

    @Test
    @DisplayName("서울 기준 내일 끝나는 마감을 찾고, 하나가 실패해도 나머지는 보낸다")
    void sendsTomorrow_continuesOnFailure() {
        DueNotice failing = due(1);
        DueNotice ok = due(2);
        given(service.findDue(LocalDate.of(2026, 11, 20))).willReturn(List.of(failing, ok));
        given(service.send(failing)).willThrow(new IllegalStateException("제약 위반"));
        given(service.send(ok)).willReturn(3);

        CompetitionDeadlineNoticeRunResponse result = scheduler.sendTomorrowDeadlines();

        verify(service).findDue(LocalDate.of(2026, 11, 20));
        verify(service).send(ok);
        assertThat(result).isEqualTo(new CompetitionDeadlineNoticeRunResponse(LocalDate.of(2026, 11, 20), 2, 1, 3));
    }
}
