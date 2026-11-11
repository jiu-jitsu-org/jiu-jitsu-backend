package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.board.service.BoardHideService;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.notice.dto.CompetitionNoticePromptRequest;
import com.jiujitsu.api.domain.notice.dto.NoticeListResponse;
import com.jiujitsu.api.domain.notice.dto.NoticeSettingResponse;
import com.jiujitsu.api.domain.notice.entity.Notice;
import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import com.jiujitsu.api.domain.notice.factory.NoticeFactory;
import com.jiujitsu.api.domain.notice.repository.ContentNoticeSettingRepository;
import com.jiujitsu.api.domain.notice.repository.NoticeRepository;
import com.jiujitsu.api.domain.notice.repository.UserNoticeSettingRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 알림함의 대회 알림 (#189 #190): data 가 대회 id 라 게시글 기준 필터·삭제 상태에 걸리면 안 된다
 */
@ExtendWith(MockitoExtension.class)
class NoticeServiceCompetitionTest {

    private static final ZoneId SEOUL = ZoneId.of("Asia/Seoul");

    @InjectMocks private NoticeService noticeService;

    @Mock private NoticeRepository noticeRepository;
    @Mock private NoticeFactory noticeFactory;
    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private UserNoticeSettingRepository userNoticeSettingRepository;
    @Mock private ContentNoticeSettingRepository contentNoticeSettingRepository;
    @Mock private ContentRepository contentRepository;
    @Mock private CommunityCommentsRepository communityCommentsRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private UserBlockService userBlockService;
    @Mock private BoardHideService boardHideService;
    @Mock private ReportService reportService;
    @Mock private CompetitionRepository competitionRepository;
    @Spy private Clock clock = Clock.fixed(Instant.parse("2026-11-11T09:00:00Z"), SEOUL);

    private final User me = User.builder().id(1L).role(UserRole.USER).build();

    @BeforeEach
    void login() {
        lenient().when(authenticationFacade.getCurrentUser()).thenReturn(me);
    }

    private static Notice competitionNotice(long id, String competitionId) {
        return Notice.builder().id(id).userId(1L)
                .pushType(FcmPushType.COMPETITION_EARLY_BIRD_DEADLINE)
                .pushActionType(PushActionType.COMPETITION_DETAIL)
                .title("얼리버드 마감 내일")
                .body("대회" + competitionId + " 얼리버드 마감이 내일이에요. 신청과 결제는 대회 공지에서")
                .data(competitionId)
                .isRead(false)
                .build();
    }

    private static Notice boardNotice(long id, String contentId) {
        return Notice.builder().id(id).userId(1L)
                .pushType(FcmPushType.CONTENTS_LIKE)
                .pushActionType(PushActionType.BOARD_DETAIL)
                .title("내 게시글에 좋아요가 달렸어요.")
                .data(contentId)
                .isRead(false)
                .build();
    }

    @Test
    @DisplayName("목록: 대회 알림은 같은 번호 게시글을 숨겼어도 보이고, 저장된 본문·대회 상태(없거나 미공개면 COMPETITION)를 준다")
    void list_competitionNotices() {
        // 게시글 7 을 숨김 → 게시글 알림(7)은 빠지지만 대회 7 알림은 남아야 한다
        given(boardHideService.getHiddenContentIds()).willReturn(List.of(7L));
        given(reportService.getReportedTargetIds(ReportType.BOARD)).willReturn(List.of());
        given(userBlockService.getBlockedUserIds()).willReturn(List.of());
        given(noticeRepository.findByUserIdOrderByNotifiedAtDescIdDesc(1L)).willReturn(List.of(
                competitionNotice(1, "7"), competitionNotice(2, "8"), boardNotice(3, "7")));
        given(competitionRepository.findAllByIdInAndPublishedTrue(Set.of(7L, 8L)))
                .willReturn(List.of(Competition.builder().id(7L).name("대회7").published(true).build()));

        List<NoticeListResponse> list = noticeService.getNoticeList();

        assertThat(list).extracting(NoticeListResponse::id).containsExactly(1L, 2L);
        assertThat(list.get(0).actionType()).isEqualTo(PushActionType.COMPETITION_DETAIL);
        assertThat(list.get(0).data()).isEqualTo("7");
        assertThat(list.get(0).body()).isEqualTo("대회7 얼리버드 마감이 내일이에요. 신청과 결제는 대회 공지에서");
        assertThat(list.get(0).deletedStatus()).isNull();
        assertThat(list.get(1).deletedStatus()).isEqualTo("COMPETITION");
        // 대회 id 를 게시글(content) 존재 확인에 섞지 않는다
        verify(contentRepository, never()).findAllById(any());
    }

    @Test
    @DisplayName("안 읽은 수: 대회 알림도 센다 (게시글 숨김과 번호가 같아도)")
    void unreadCount_includesCompetition() {
        given(boardHideService.getHiddenContentIds()).willReturn(List.of(7L));
        given(reportService.getReportedTargetIds(ReportType.BOARD)).willReturn(List.of());
        given(userBlockService.getBlockedUserIds()).willReturn(List.of());
        given(noticeRepository.findByUserIdAndIsReadFalse(1L)).willReturn(List.of(competitionNotice(1, "7"), boardNotice(3, "7")));

        assertThat(noticeService.getUnreadCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("관심 대회 알림 질문 답: 값과 물어본 시각을 저장해 설정 응답으로 준다")
    void answerPrompt() {
        UserNoticeSetting setting = new UserNoticeSetting(me, false);
        User withSetting = User.builder().id(1L).role(UserRole.USER).userNoticeSetting(setting).build();
        given(authenticationFacade.getCurrentUser()).willReturn(withSetting);

        NoticeSettingResponse response = noticeService.answerCompetitionNoticePrompt(new CompetitionNoticePromptRequest(true));

        assertThat(response.competitionEnabled()).isTrue();
        assertThat(response.competitionNoticeAsked()).isTrue();
        assertThat(setting.getCompetitionNoticeAskedAt()).isEqualTo(LocalDateTime.of(2026, 11, 11, 18, 0));
        verify(userNoticeSettingRepository).save(setting);
    }
}
