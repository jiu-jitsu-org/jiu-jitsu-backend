package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceResultNoticeRunResponse;
import com.jiujitsu.api.domain.community.balance_game.service.BalanceResultNoticeService.DueNotice;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.support.CronExpression;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class BalanceResultNoticeRunServiceTest {

    private static final LocalDate YESTERDAY = LocalDate.of(2026, 11, 25);

    // 서울 2026-11-26 11:55
    private final Clock clock = Clock.fixed(Instant.parse("2026-11-26T02:55:00Z"), ZoneId.of("Asia/Seoul"));
    private final BalanceResultNoticeService service = mock(BalanceResultNoticeService.class);
    private final BalanceResultNoticeRunService runService = new BalanceResultNoticeRunService(service, clock);

    private static DueNotice due(long id) {
        return new DueNotice(id, id * 10, "투표 결과: 타이틀" + id, "A vs B");
    }

    @Test
    @DisplayName("서울 기준 어제 마감된 판을 찾고, 판 하나가 실패해도 나머지는 보낸다")
    void sendsYesterday_continuesOnFailure() {
        DueNotice failing = due(1);
        DueNotice ok = due(2);
        given(service.findDue(YESTERDAY)).willReturn(List.of(failing, ok));
        given(service.send(failing)).willThrow(new IllegalStateException("제약 위반"));
        given(service.send(ok)).willReturn(3);

        assertThat(runService.runYesterday()).isEqualTo(new BalanceResultNoticeRunResponse(YESTERDAY, 2, 3, 1));
        verify(service).send(ok);
    }

    @Test
    @DisplayName("어제 마감된 판이 없으면 아무것도 보내지 않는다")
    void nothingDue() {
        given(service.findDue(YESTERDAY)).willReturn(List.of());

        assertThat(runService.runYesterday()).isEqualTo(new BalanceResultNoticeRunResponse(YESTERDAY, 0, 0, 0));
        verify(service, never()).send(any());
    }

    @Test
    @DisplayName("날짜는 서울 기준 — UTC 로는 아직 전날인 00:30 에 돌려도 서울 어제를 본다 (놓친 판은 날이 지나면 보내지 않음)")
    void seoulDate() {
        Clock afterMidnight = Clock.fixed(Instant.parse("2026-11-25T15:30:00Z"), ZoneId.of("Asia/Seoul")); // 서울 11/26 00:30
        new BalanceResultNoticeRunService(service, afterMidnight).runYesterday();

        verify(service).findDue(YESTERDAY);
    }

    @Test
    @DisplayName("기본 실행 시각은 매일 11:55 (서울), 최소 투표 5 — application.yml 값")
    void defaultSchedule() throws Exception {
        PropertySource<?> yml = new YamlPropertySourceLoader().load("application", new ClassPathResource("application.yml")).get(0);
        Scheduled scheduled = BalanceResultNoticeRunService.class.getMethod("runDaily").getAnnotation(Scheduled.class);

        assertThat(scheduled.cron()).isEqualTo("${balance-game.result-notice.cron}");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
        assertThat(CronExpression.parse(yml.getProperty("balance-game.result-notice.cron").toString())
                .next(LocalDateTime.of(2026, 11, 26, 0, 0))).isEqualTo(LocalDateTime.of(2026, 11, 26, 11, 55));
        assertThat(yml.getProperty("balance-game.result-notice.min-votes")).isEqualTo(5);
    }
}
