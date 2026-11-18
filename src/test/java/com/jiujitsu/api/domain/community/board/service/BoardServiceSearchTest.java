package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.BoardListRequest;
import com.jiujitsu.api.domain.community.board.dto.BoardListResponse;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardListType;
import com.jiujitsu.api.domain.community.board.factory.BoardFactory;
import com.jiujitsu.api.domain.community.board.mapper.BoardMapper;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.competition.service.CompetitionQueryService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 게시글 목록의 검색(SEARCH) 분기 (#265), 대회 조건 (#269)
 */
@ExtendWith(MockitoExtension.class)
class BoardServiceSearchTest {

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

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    @DisplayName("검색어가 없거나 빈 값 · 공백만이면 조회하지 않고 빈 페이지 (전에는 전체 글)")
    void blankKeyword_emptyPage() {
        for (String keyword : new String[]{null, "", "   ", "　"}) {
            Page<BoardListResponse> result = boardService.getList(new BoardListRequest(BoardListType.SEARCH, null, keyword, null), pageable);
            assertThat(result.getContent()).isEmpty();
            assertThat(result.getTotalElements()).isZero();
        }
        verify(boardRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @DisplayName("검색어가 있으면 조건(Specification)으로 조회")
    @SuppressWarnings("unchecked")
    void keyword_queriesBySpecification() {
        given(boardRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(new PageImpl<Board>(List.of(), pageable, 0));

        boardService.getList(new BoardListRequest(BoardListType.SEARCH, null, "오픈매트 강남", null), pageable);

        verify(boardRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("대회 조건: 공개 대회면 조회, 없거나 비공개면 COMPETITION_NOT_FOUND 이고 목록을 조회하지 않음 (#269)")
    @SuppressWarnings("unchecked")
    void competition_publishedOnly() {
        given(boardRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(new PageImpl<Board>(List.of(), pageable, 0));
        given(competitionQueryService.getPublished(anyLong())).willAnswer(inv -> {
            if (inv.getArgument(0, Long.class) == 9L) {
                throw new ErrorException(ErrorCode.COMPETITION_NOT_FOUND);
            }
            return null;
        });

        boardService.getList(new BoardListRequest(BoardListType.CATEGORY, 5L, null, 7L), pageable);
        verify(competitionQueryService).getPublished(7L);
        verify(boardRepository).findAll(any(Specification.class), eq(pageable));

        assertThatThrownBy(() -> boardService.getList(new BoardListRequest(BoardListType.FEED, null, null, 9L), pageable))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
        verifyNoMoreInteractions(boardRepository);
    }

    @Test
    @DisplayName("대회 조건 없이 부르면 대회를 조회하지 않음, CATEGORY 인데 categoryId 가 없으면 빈 페이지 (전과 같음)")
    @SuppressWarnings("unchecked")
    void noCompetition() {
        given(boardRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(new PageImpl<Board>(List.of(), pageable, 0));

        boardService.getList(new BoardListRequest(BoardListType.FEED, null, null, null), pageable);
        assertThat(boardService.getList(new BoardListRequest(BoardListType.CATEGORY, null, null, null), pageable).getContent()).isEmpty();

        verify(competitionQueryService, never()).getPublished(any());
        verify(boardRepository, times(1)).findAll(any(Specification.class), any(Pageable.class));
    }
}
