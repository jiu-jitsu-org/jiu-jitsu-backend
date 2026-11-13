package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.notice.entity.CompetitionDeadlineNoticeLog;
import com.jiujitsu.api.domain.notice.entity.CompetitionDeadlineType;
import com.jiujitsu.api.domain.notice.repository.CompetitionDeadlineNoticeLogRepository;
import com.jiujitsu.api.domain.notice.repository.UserNoticeSettingRepository;
import com.jiujitsu.api.domain.notice.service.CompetitionDeadlineNoticeService.DueNotice;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
import com.jiujitsu.api.domain.competition.entity.Competition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

@ExtendWith(MockitoExtension.class)
class CompetitionDeadlineNoticeServiceTest {

    private static final LocalDate D = LocalDate.of(2026, 11, 20);
    private static final Map<String, String> DATA = Map.of("type", "COMPETITION_DETAIL", "data", "7");

    @InjectMocks private CompetitionDeadlineNoticeService service;

    @Mock private CompetitionRepository competitionRepository;
    @Mock private UserNoticeSettingRepository userNoticeSettingRepository;
    @Mock private CompetitionDeadlineNoticeLogRepository logRepository;
    @Mock private NoticeService noticeService;
    @Mock private FcmPushEventPublisher fcmPushEventPublisher;

    private final DueNotice earlyBird = new DueNotice(7L, "서울 오픈",
            new CompetitionDeadline(CompetitionDeadlineType.EARLY_BIRD, "1차 얼리버드", D));

    // 보내기 직전 공개 여부 확인 — 기본은 공개 (findDue 만 부르는 테스트도 있어 lenient)
    @BeforeEach
    void publishedByDefault() {
        lenient().when(competitionRepository.findByIdAndPublishedTrue(anyLong()))
                .thenReturn(Optional.of(Competition.builder().id(7L).published(true).build()));
    }

    @Test
    @DisplayName("보내기: 아직 안 받은 유저에게만 기록·알림·푸시, 문구와 대회 상세 딥링크")
    void send_onlyNotNotified() {
        given(userNoticeSettingRepository.findCompetitionNoticeRecipientIds(7L)).willReturn(List.of(1L, 2L, 3L));
        given(logRepository.findNotifiedUserIds(7L, CompetitionDeadlineType.EARLY_BIRD, D, List.of(1L, 2L, 3L))).willReturn(Set.of(2L));

        assertThat(service.send(earlyBird)).isEqualTo(2);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<CompetitionDeadlineNoticeLog>> logs = ArgumentCaptor.forClass(List.class);
        verify(logRepository).saveAllAndFlush(logs.capture());
        assertThat(logs.getValue()).extracting(CompetitionDeadlineNoticeLog::getUserId).containsExactly(1L, 3L);
        assertThat(logs.getValue()).allSatisfy(log -> {
            assertThat(log.getCompetitionId()).isEqualTo(7L);
            assertThat(log.getDeadlineType()).isEqualTo(CompetitionDeadlineType.EARLY_BIRD);
            assertThat(log.getDeadline()).isEqualTo(D);
        });

        String title = "1차 얼리버드 마감 내일";
        String body = "서울 오픈 1차 얼리버드 마감이 내일이에요. 신청과 결제는 대회 공지에서";
        for (Long userId : List.of(1L, 3L)) {
            verify(noticeService).saveNoticeWithText(userId, FcmPushType.COMPETITION_EARLY_BIRD_DEADLINE, DATA, title, body);
            verify(fcmPushEventPublisher).publish(userId, FcmPushType.COMPETITION_EARLY_BIRD_DEADLINE, DATA, title, body);
        }
        verify(noticeService, never()).saveNoticeWithText(eq(2L), any(), any(), any(), any());
    }

    @Test
    @DisplayName("보내기: 접수 마감은 접수 알림 종류로")
    void send_registration() {
        DueNotice registration = new DueNotice(7L, "서울 오픈", new CompetitionDeadline(CompetitionDeadlineType.REGISTRATION, "접수", D));
        given(userNoticeSettingRepository.findCompetitionNoticeRecipientIds(7L)).willReturn(List.of(1L));
        given(logRepository.findNotifiedUserIds(any(), any(), any(), any())).willReturn(Set.of());

        service.send(registration);

        verify(fcmPushEventPublisher).publish(1L, FcmPushType.COMPETITION_REGISTRATION_DEADLINE, DATA,
                "접수 마감 내일", "서울 오픈 접수가 내일 끝나요. 신청과 결제는 대회 공지에서");
    }

    @Test
    @DisplayName("보내기: 받을 사람이 없으면 아무것도 하지 않는다 / 모두 이미 받았으면 0건")
    void send_nobody() {
        given(userNoticeSettingRepository.findCompetitionNoticeRecipientIds(7L)).willReturn(List.of());
        assertThat(service.send(earlyBird)).isZero();
        verifyNoInteractions(logRepository, noticeService, fcmPushEventPublisher);

        given(userNoticeSettingRepository.findCompetitionNoticeRecipientIds(7L)).willReturn(List.of(1L));
        given(logRepository.findNotifiedUserIds(any(), any(), any(), any())).willReturn(Set.of(1L));
        assertThat(service.send(earlyBird)).isZero();
        verify(noticeService, never()).saveNoticeWithText(anyLong(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("대상 목록: 그날 끝나는 대회마다 마감 계산 (대회 하나에 마감 하나)")
    void findDue() {
        Competition seoul = Competition.builder().id(7L).name("서울 오픈").published(true).registrationDeadline(D).build();
        Competition busan = Competition.builder().id(8L).name("부산 오픈").published(true).earlyBirdDeadline(D).registrationDeadline(D.plusDays(3)).build();
        given(competitionRepository.findPublishedWithDeadlineOn(D)).willReturn(List.of(seoul, busan));

        assertThat(service.findDue(D)).containsExactly(
                new DueNotice(7L, "서울 오픈", new CompetitionDeadline(CompetitionDeadlineType.REGISTRATION, "접수", D)),
                new DueNotice(8L, "부산 오픈", new CompetitionDeadline(CompetitionDeadlineType.EARLY_BIRD, "얼리버드", D)));
    }

    @Test
    @DisplayName("보내기: 목록을 뽑은 뒤 비공개로 바뀐 대회는 보내지 않는다")
    void send_skipsUnpublished() {
        given(competitionRepository.findByIdAndPublishedTrue(7L)).willReturn(Optional.empty());

        assertThat(service.send(earlyBird)).isZero();
        verifyNoInteractions(userNoticeSettingRepository, logRepository, noticeService, fcmPushEventPublisher);
    }
}

