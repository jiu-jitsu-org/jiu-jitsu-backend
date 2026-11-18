package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.factory.BoardFactory;
import com.jiujitsu.api.domain.community.board.mapper.BoardMapper;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.competition.service.CompetitionQueryService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 글 상세를 열면 그 글에 대한 내 안 읽은 알림을 읽음 처리 (#246)
@ExtendWith(MockitoExtension.class)
class BoardServiceReadNoticeTest {

    private static final Long CONTENT_ID = 100L;

    @InjectMocks
    private BoardService boardService;

    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private UserRepository userRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private BoardFactory boardFactory;
    @Mock private BoardMapper boardMapper;
    @Mock private BoardCategoryService boardCategoryService;
    @Mock private CommunityCommentsService communityCommentsService;
    @Mock private ContentService contentService;
    @Mock private UserBlockService userBlockService;
    @Mock private NoticeService noticeService;
    @Mock private ReportService reportService;
    @Mock private BoardHideService boardHideService;
    @Mock private CommunityProfileService communityProfileService;
    @Mock private CompetitionQueryService competitionQueryService;

    private final User me = User.builder().id(1L).role(UserRole.USER).build();

    private Board board(boolean hidden) {
        Content content = mock(Content.class);
        lenient().when(content.getId()).thenReturn(CONTENT_ID);
        Board board = mock(Board.class);
        lenient().when(board.getContent()).thenReturn(content);
        lenient().when(board.getCreatedBy()).thenReturn(User.builder().id(2L).build());
        lenient().when(board.isHidden()).thenReturn(hidden);
        given(boardRepository.findByContent_Id(CONTENT_ID)).willReturn(Optional.of(board));
        return board;
    }

    @Test
    @DisplayName("로그인한 사람이 글 상세를 열면 그 글에 대한 내 알림을 읽음 처리")
    void loggedIn_readsNotices() {
        board(false);
        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.of(me));

        boardService.getById(CONTENT_ID);

        verify(noticeService).readContentNotices(1L, CONTENT_ID);
    }

    @Test
    @DisplayName("비로그인은 읽음 처리할 알림이 없음")
    void guest_noRead() {
        board(false);

        boardService.getById(CONTENT_ID);

        verify(noticeService, never()).readContentNotices(anyLong(), anyLong());
    }

    @Test
    @DisplayName("열리지 않은 글(숨김 · 내가 신고)은 읽음 처리하지 않음")
    void notOpened_noRead() {
        board(true);
        assertThatThrownBy(() -> boardService.getById(CONTENT_ID)).isInstanceOf(ErrorException.class);

        board(false);
        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.of(me));
        given(reportService.hasReported(me, ReportType.BOARD, CONTENT_ID)).willReturn(true);
        assertThatThrownBy(() -> boardService.getById(CONTENT_ID)).isInstanceOf(ErrorException.class);

        verify(noticeService, never()).readContentNotices(anyLong(), anyLong());
    }
}
