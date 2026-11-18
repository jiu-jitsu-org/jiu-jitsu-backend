package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.BoardCreateRequest;
import com.jiujitsu.api.domain.community.board.dto.BoardUpdateRequest;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.factory.BoardFactory;
import com.jiujitsu.api.domain.community.board.mapper.BoardMapper;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.competition.service.CompetitionQueryService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 글 내용 선택 (#264) — 없는 내용은 "" 로 저장, 수정에서 빈 값은 내용 지움
 */
@ExtendWith(MockitoExtension.class)
class BoardServiceBodyOptionalTest {

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

    @Test
    @DisplayName("등록: 내용이 없거나(null) 빈 값 · 공백만이면 \"\" 로 저장, 있으면 그대로")
    void create_bodyOptional() {
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(boardFactory.createBoard(any(), any(), any(), any(), any(), any())).willAnswer(inv -> mock(Board.class));
        given(boardRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        boardService.create(new BoardCreateRequest(1L, "제목만", null, null, null, null));
        boardService.create(new BoardCreateRequest(1L, "제목만", "", null, null, null));
        boardService.create(new BoardCreateRequest(1L, "제목만", "  \n", null, null, null));
        boardService.create(new BoardCreateRequest(1L, "제목", "본문", null, null, null));

        verify(boardFactory, times(3)).createBoard(any(), any(), any(), eq("제목만"), eq(""), any());
        verify(boardFactory).createBoard(any(), any(), any(), eq("제목"), eq("본문"), any());
    }

    @Test
    @DisplayName("수정: 빈 값 · 공백만은 내용을 지우고(\"\"), null 은 그대로, 값은 교체")
    void update_bodyClearKeepReplace() {
        Board board = myBoard();
        given(boardRepository.findByContent_Id(100L)).willReturn(Optional.of(board));

        boardService.update(100L, bodyRequest(null));
        assertThat(board.getBody()).isEqualTo("본문");

        boardService.update(100L, bodyRequest("새 본문"));
        assertThat(board.getBody()).isEqualTo("새 본문");

        boardService.update(100L, bodyRequest("   "));
        assertThat(board.getBody()).isEmpty();

        boardService.update(100L, bodyRequest("다시"));
        boardService.update(100L, bodyRequest(""));
        assertThat(board.getBody()).isEmpty();
    }

    private static BoardUpdateRequest bodyRequest(String body) {
        return new BoardUpdateRequest(null, null, body, null, null, null);
    }

    private Board myBoard() {
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        Content content = mock(Content.class);
        lenient().when(content.getImageFiles()).thenReturn(new ArrayList<>());
        Board board = Board.builder().id(10L).content(content).title("제목").body("본문").build();
        ReflectionTestUtils.setField(board, "createdBy", me);
        return board;
    }
}
