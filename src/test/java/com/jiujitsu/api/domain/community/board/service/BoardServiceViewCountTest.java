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
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

// 글 조회수는 상세 조회가 아니라 '봤음' 요청(POST /board/{id}/views)으로만 (#249)
@ExtendWith(MockitoExtension.class)
class BoardServiceViewCountTest {

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

    private final User author = User.builder().id(2L).role(UserRole.USER).build();
    private final User reader = User.builder().id(1L).role(UserRole.USER).build();

    private void board(boolean hidden) {
        Content content = mock(Content.class);
        lenient().when(content.getId()).thenReturn(CONTENT_ID);
        Board board = mock(Board.class);
        lenient().when(board.getContent()).thenReturn(content);
        lenient().when(board.getCreatedBy()).thenReturn(author);
        lenient().when(board.isHidden()).thenReturn(hidden);
        given(boardRepository.findByContent_Id(CONTENT_ID)).willReturn(Optional.of(board));
    }

    @Test
    @DisplayName("상세 조회는 조회수를 올리지 않음 — 같은 방문 안에서 여러 번 불려도 그대로")
    void detail_doesNotCount() {
        board(false);
        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.of(reader));

        boardService.getById(CONTENT_ID);
        boardService.getById(CONTENT_ID);

        verify(contentService, never()).increaseViewCount(anyLong(), anyBoolean());
    }

    @Test
    @DisplayName("'봤음': 남이 보면 매번 +1, 비로그인도 셈")
    void view_otherOrGuest_counts() {
        board(false);
        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.of(reader));
        boardService.increaseView(CONTENT_ID);
        verify(contentService).increaseViewCount(CONTENT_ID, false);

        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.empty());
        boardService.increaseView(CONTENT_ID);
        verify(contentService, org.mockito.Mockito.times(2)).increaseViewCount(CONTENT_ID, false);
    }

    @Test
    @DisplayName("'봤음': 작성자 본인은 첫 1회만 세는 쪽으로")
    void view_author_once() {
        board(false);
        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.of(author));

        boardService.increaseView(CONTENT_ID);

        verify(contentService).increaseViewCount(CONTENT_ID, true);
    }

    @Test
    @DisplayName("'봤음': 상세와 같은 기준으로 볼 수 없는 글(숨김 · 내가 신고)은 BOARD_NOT_FOUND, 세지 않음")
    void view_notViewable_404() {
        board(true);
        assertThatThrownBy(() -> boardService.increaseView(CONTENT_ID))
                .isInstanceOf(ErrorException.class).extracting("errorCode").isEqualTo(ErrorCode.BOARD_NOT_FOUND);

        board(false);
        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.of(reader));
        given(reportService.hasReported(reader, ReportType.BOARD, CONTENT_ID)).willReturn(true);
        assertThatThrownBy(() -> boardService.increaseView(CONTENT_ID))
                .isInstanceOf(ErrorException.class).extracting("errorCode").isEqualTo(ErrorCode.BOARD_NOT_FOUND);

        verify(contentService, never()).increaseViewCount(anyLong(), anyBoolean());
    }
}
