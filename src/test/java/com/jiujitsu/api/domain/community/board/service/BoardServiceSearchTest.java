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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 게시글 목록의 검색(SEARCH) 분기 (#265), 0건 검색어 기록 (#266)
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
    @Mock private BoardSearchMissService boardSearchMissService;

    private final Pageable pageable = PageRequest.of(0, 20);

    @Test
    @DisplayName("검색어가 없거나 빈 값 · 공백만이면 조회하지 않고 빈 페이지 (전에는 전체 글)")
    void blankKeyword_emptyPage() {
        for (String keyword : new String[]{null, "", "   ", "　"}) {
            Page<BoardListResponse> result = boardService.getList(new BoardListRequest(BoardListType.SEARCH, null, keyword), pageable);
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

        boardService.getList(new BoardListRequest(BoardListType.SEARCH, null, "오픈매트 강남"), pageable);

        verify(boardRepository).findAll(any(Specification.class), eq(pageable));
    }

    @Test
    @DisplayName("첫 페이지가 0건이면 검색 단어로 0건 기록 (#266)")
    @SuppressWarnings("unchecked")
    void zeroResult_firstPage_recorded() {
        given(boardRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(new PageImpl<Board>(List.of(), pageable, 0));

        boardService.getList(new BoardListRequest(BoardListType.SEARCH, null, " 베림보로  NoGi "), pageable);

        verify(boardSearchMissService).record(List.of("베림보로", "nogi"));
    }

    @Test
    @DisplayName("다음 페이지 요청이나 결과가 있으면 기록하지 않음 (#266)")
    @SuppressWarnings("unchecked")
    void notRecorded_nextPageOrFound() {
        Pageable second = PageRequest.of(1, 20);
        given(boardRepository.findAll(any(Specification.class), eq(second))).willReturn(new PageImpl<Board>(List.of(), second, 0));
        given(boardRepository.findAll(any(Specification.class), eq(pageable))).willReturn(new PageImpl<Board>(List.of(), pageable, 3));

        boardService.getList(new BoardListRequest(BoardListType.SEARCH, null, "베림보로"), second);
        boardService.getList(new BoardListRequest(BoardListType.SEARCH, null, "오픈매트"), pageable);

        verify(boardSearchMissService, never()).record(any());
    }

    @Test
    @DisplayName("0건 기록이 실패해도 검색 응답은 그대로 (#266)")
    @SuppressWarnings("unchecked")
    void recordFailure_searchStillReturns() {
        given(boardRepository.findAll(any(Specification.class), any(Pageable.class))).willReturn(new PageImpl<Board>(List.of(), pageable, 0));
        doThrow(new RuntimeException("db down")).when(boardSearchMissService).record(any());

        Page<BoardListResponse> result = boardService.getList(new BoardListRequest(BoardListType.SEARCH, null, "베림보로"), pageable);

        assertThat(result.getContent()).isEmpty();
    }
}
