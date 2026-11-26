package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameRepository;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameVoteRepository;
import com.jiujitsu.api.domain.community.balance_game.service.BalanceResultNoticeService.DueNotice;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
import com.jiujitsu.api.global.properties.BalanceResultNoticeProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 밸런스 게임 결과 알림 — 대상 판 · 문구 · 보낼 사람 (#291)
 */
@ExtendWith(MockitoExtension.class)
class BalanceResultNoticeServiceTest {

    // 서울 2026-11-26 11:55
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 11, 26, 11, 55);
    private static final Map<String, String> DATA = Map.of("type", "BALANCE_DETAIL", "data", "100");

    @Mock private BalanceGameRepository balanceGameRepository;
    @Mock private BalanceGameVoteRepository balanceGameVoteRepository;
    @Mock private NoticeService noticeService;
    @Mock private FcmPushEventPublisher fcmPushEventPublisher;

    private BalanceResultNoticeService service;

    private final DueNotice due = new DueNotice(10L, 100L, "투표 결과: 더 싫은 패배는?", "매번 서브미션패 vs 매번 포인트패");

    @BeforeEach
    void setUp() {
        service = new BalanceResultNoticeService(balanceGameRepository, balanceGameVoteRepository, noticeService, fcmPushEventPublisher,
                new BalanceResultNoticeProperties(5), Clock.fixed(Instant.parse("2026-11-26T02:55:00Z"), ZoneId.of("Asia/Seoul")));
    }

    private static BalanceGame game(String title) {
        return BalanceGame.builder()
                .id(10L)
                .content(Content.builder().id(100L).contentType(ContentType.BALANCE).build())
                .title(title)
                .optionAText("매번 서브미션패")
                .optionBText("매번 포인트패")
                .build();
    }

    private static BalanceGameVoteRepository.ResultNoticeTarget target(Long voteId, Long userId) {
        return new BalanceGameVoteRepository.ResultNoticeTarget() {
            public Long getVoteId() { return voteId; }
            public Long getUserId() { return userId; }
        };
    }

    private void givenDue(BalanceGame... games) {
        given(balanceGameRepository.findClosedBetweenWithMinVotes(any(), any(), eq(5L))).willReturn(List.of(games));
    }

    @Test
    @DisplayName("대상 판: 그날 00:00 부터 다음 날 00:00 전까지 마감, 총 투표 기준은 설정 값")
    void findDue_range() {
        givenDue(game("더 싫은 패배는?"));

        assertThat(service.findDue(LocalDate.of(2026, 11, 25))).containsExactly(due);
        verify(balanceGameRepository).findClosedBetweenWithMinVotes(
                LocalDateTime.of(2026, 11, 25, 0, 0), LocalDateTime.of(2026, 11, 26, 0, 0), 5L);
    }

    @Test
    @DisplayName("타이틀이 없는 판은 제목이 기본 문구, 본문은 그대로 선택지")
    void findDue_withoutTitle() {
        givenDue(game(null), game(" "));

        assertThat(service.findDue(LocalDate.of(2026, 11, 25)))
                .extracting(DueNotice::title, DueNotice::body)
                .containsOnly(org.assertj.core.groups.Tuple.tuple("밸런스 게임 결과가 나왔어요", "매번 서브미션패 vs 매번 포인트패"));
    }

    @Test
    @DisplayName("보내기: 보낸 기록을 남긴 사람에게만 알림함 · 푸시를 같은 문구로, 밸런스 게임 상세로")
    void send() {
        given(balanceGameVoteRepository.findResultNoticeTargets(10L)).willReturn(List.of(target(1L, 11L), target(2L, 12L), target(3L, 13L)));
        given(balanceGameVoteRepository.markResultNoticed(1L, NOW)).willReturn(1);
        given(balanceGameVoteRepository.markResultNoticed(2L, NOW)).willReturn(0);   // 다른 실행이 먼저 보냈거나 그 사이 봄
        given(balanceGameVoteRepository.markResultNoticed(3L, NOW)).willReturn(1);

        assertThat(service.send(due)).isEqualTo(2);

        for (Long userId : List.of(11L, 13L)) {
            verify(noticeService).saveNoticeWithText(userId, FcmPushType.BALANCE_RESULT, DATA, due.title(), due.body());
            verify(fcmPushEventPublisher).publish(userId, FcmPushType.BALANCE_RESULT, DATA, due.title(), due.body());
        }
        verify(noticeService, never()).saveNoticeWithText(eq(12L), any(), any(), any(), any());
        verify(fcmPushEventPublisher, never()).publish(eq(12L), any(), any(), any(), any());
    }

    @Test
    @DisplayName("보내기: 받을 사람이 없으면 아무것도 하지 않는다")
    void send_nobody() {
        given(balanceGameVoteRepository.findResultNoticeTargets(10L)).willReturn(List.of());

        assertThat(service.send(due)).isZero();
        verifyNoInteractions(noticeService, fcmPushEventPublisher);
    }
}
