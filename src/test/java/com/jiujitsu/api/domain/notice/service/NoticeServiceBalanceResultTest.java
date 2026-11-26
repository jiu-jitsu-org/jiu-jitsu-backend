package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.board.service.BoardHideService;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.notice.dto.NoticeListResponse;
import com.jiujitsu.api.domain.notice.entity.Notice;
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
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;

/**
 * 알림함의 밸런스 게임 결과 알림 (#291)
 */
@ExtendWith(MockitoExtension.class)
class NoticeServiceBalanceResultTest {

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

    @Test
    @DisplayName("목록: 보낼 때 저장한 제목 · 본문과 이동할 판(contentId)을 준다")
    void list() {
        given(boardHideService.getHiddenContentIds()).willReturn(List.of());
        given(reportService.getReportedTargetIds(any())).willReturn(List.of());
        given(userBlockService.getBlockedUserIds()).willReturn(List.of());
        given(noticeRepository.findByUserIdOrderByNotifiedAtDescIdDesc(1L)).willReturn(List.of(Notice.builder().id(1L).userId(1L)
                .pushType(FcmPushType.BALANCE_RESULT)
                .pushActionType(PushActionType.BALANCE_DETAIL)
                .title("투표 결과: 더 싫은 패배는?")
                .body("매번 서브미션패 vs 매번 포인트패")
                .data("100")
                .isRead(false)
                .build()));
        given(contentRepository.findAllById(Set.of(100L))).willReturn(List.of(Content.builder().id(100L).contentType(ContentType.BALANCE).build()));

        List<NoticeListResponse> list = noticeService.getNoticeList();

        assertThat(list).hasSize(1);
        NoticeListResponse notice = list.get(0);
        assertThat(notice.title()).isEqualTo("투표 결과: 더 싫은 패배는?");
        assertThat(notice.body()).isEqualTo("매번 서브미션패 vs 매번 포인트패");
        assertThat(notice.pushType()).isEqualTo(FcmPushType.BALANCE_RESULT);
        assertThat(notice.actionType()).isEqualTo(PushActionType.BALANCE_DETAIL);
        assertThat(notice.data()).isEqualTo("100");
        assertThat(notice.commentId()).isNull();
        assertThat(notice.deletedStatus()).isNull();
    }

    @Test
    @DisplayName("결과 봤음: 그 판의 결과 알림 종류만 읽음 처리한다")
    void readBalanceResultNotice() {
        noticeService.readBalanceResultNotice(1L, 100L);

        verify(noticeRepository).markReadByUserIdAndContentAndPushType(1L, "100", FcmPushType.BALANCE_RESULT);
    }
}
