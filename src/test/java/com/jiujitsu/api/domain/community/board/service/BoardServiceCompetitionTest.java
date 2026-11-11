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
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.competition.dto.CompetitionListItem;
import com.jiujitsu.api.domain.competition.entity.Competition;
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
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 게시글 ↔ 대회 연결, 게시글 대회 카드, 대회 이야기 (#188)
 */
@ExtendWith(MockitoExtension.class)
class BoardServiceCompetitionTest {

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

    // ---------- 글쓰기 ----------

    @Test
    @DisplayName("글쓰기: competitionId 가 있으면 공개 대회를 연결하고, 없으면 연결하지 않는다")
    void create_linksPublishedCompetition() {
        Competition competition = competition(7L);
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(competitionQueryService.getPublished(7L)).willReturn(competition);
        given(boardFactory.createBoard(any(), any(), any(), any(), any(), any())).willAnswer(inv -> mock(Board.class));
        given(boardRepository.save(any())).willAnswer(inv -> inv.getArgument(0));

        boardService.create(new BoardCreateRequest(1L, "제목", "본문", null, null, 7L));
        boardService.create(new BoardCreateRequest(1L, "제목", "본문", null, null, null));

        verify(boardFactory).createBoard(any(), any(), eq(competition), any(), any(), any());
        verify(boardFactory).createBoard(any(), any(), isNull(), any(), any(), any());
        verify(competitionQueryService, times(1)).getPublished(any());
    }

    @Test
    @DisplayName("글쓰기: 없거나 미공개 대회면 COMPETITION_NOT_FOUND, 글은 저장하지 않는다")
    void create_unpublishedCompetition_notFound() {
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(competitionQueryService.getPublished(9L)).willThrow(new ErrorException(ErrorCode.COMPETITION_NOT_FOUND));

        assertThatThrownBy(() -> boardService.create(new BoardCreateRequest(1L, "제목", "본문", null, null, 9L)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
        verify(boardRepository, never()).save(any());
    }

    // ---------- 글 수정 ----------

    @Test
    @DisplayName("수정: competitionId 는 보낸 값으로 교체 — 다른 대회로 바꾸고, null 이면 연결 해제")
    void update_replacesCompetition() {
        Board linked = myBoard(competition(7L));
        given(boardRepository.findByContent_Id(100L)).willReturn(Optional.of(linked));
        Competition other = competition(8L);
        given(competitionQueryService.getPublished(8L)).willReturn(other);

        boardService.update(100L, updateRequest(8L));
        assertThat(linked.getCompetitionId()).isEqualTo(8L);

        boardService.update(100L, updateRequest(null));
        assertThat(linked.getCompetitionId()).isNull();
    }

    @Test
    @DisplayName("수정: 지금 연결된 대회를 그대로 보내면 공개 여부를 다시 보지 않는다 (연결 뒤 미공개로 바뀐 대회도 유지)")
    void update_sameCompetition_keptWithoutRecheck() {
        Board linked = myBoard(competition(7L));
        given(boardRepository.findByContent_Id(100L)).willReturn(Optional.of(linked));

        boardService.update(100L, updateRequest(7L));

        assertThat(linked.getCompetitionId()).isEqualTo(7L);
        verify(competitionQueryService, never()).getPublished(any());
    }

    @Test
    @DisplayName("수정: 미공개 대회로 바꾸면 COMPETITION_NOT_FOUND")
    void update_unpublishedCompetition_notFound() {
        Board board = myBoard(null);
        given(boardRepository.findByContent_Id(100L)).willReturn(Optional.of(board));
        given(competitionQueryService.getPublished(9L)).willThrow(new ErrorException(ErrorCode.COMPETITION_NOT_FOUND));

        assertThatThrownBy(() -> boardService.update(100L, updateRequest(9L)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
        assertThat(board.getCompetitionId()).isNull();
    }

    @Test
    @DisplayName("수정: 남의 글이면 대회를 보기 전에 PERMISSION_DENIED")
    void update_notOwner_deniedFirst() {
        Board others = board(User.builder().id(2L).role(UserRole.USER).build(), null);
        given(boardRepository.findByContent_Id(100L)).willReturn(Optional.of(others));
        given(authenticationFacade.getCurrentUser()).willReturn(me);

        assertThatThrownBy(() -> boardService.update(100L, updateRequest(7L)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.PERMISSION_DENIED);
        verify(competitionQueryService, never()).getPublished(any());
    }

    // ---------- 게시글 대회 카드 ----------

    @Test
    @DisplayName("목록: 페이지 글들이 고른 대회를 한 번에 조회해 카드로 넘기고, 연결 없거나 미공개면 null")
    void list_cardsFetchedOnce() {
        Board first = board(me, competition(7L));
        Board second = board(me, competition(7L));
        Board unlinked = board(me, null);
        Board unpublished = board(me, competition(9L));
        Pageable pageable = PageRequest.of(0, 20);
        CompetitionListItem card = card(7L);
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        given(boardRepository.findAllByCreatedByAndNotHidden(me, pageable))
                .willReturn(new PageImpl<>(List.of(first, second, unlinked, unpublished), pageable, 4));
        given(competitionQueryService.getCards(Set.of(7L, 9L))).willReturn(Map.of(7L, card));

        boardService.getWriteList(pageable);

        verify(competitionQueryService, times(1)).getCards(any());
        verify(boardMapper).toBoardListResponse(eq(first), any(), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), eq(card));
        verify(boardMapper).toBoardListResponse(eq(second), any(), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), eq(card));
        verify(boardMapper).toBoardListResponse(eq(unlinked), any(), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), isNull());
        verify(boardMapper).toBoardListResponse(eq(unpublished), any(), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), isNull());
    }

    @Test
    @DisplayName("상세: 연결된 대회 카드를 넘긴다")
    void detail_card() {
        Board linked = board(me, competition(7L));
        CompetitionListItem card = card(7L);
        given(boardRepository.findByContent_Id(100L)).willReturn(Optional.of(linked));
        given(competitionQueryService.getCards(List.of(7L))).willReturn(Map.of(7L, card));

        boardService.getById(100L);

        verify(boardMapper).toResponse(eq(linked), any(), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), any(), anyBoolean(), eq(card));
    }

    // ---------- 대회 이야기 ----------

    @Test
    @DisplayName("대회 이야기: 반응 상위 2개 다음에 최신 글로 채워 5개, 상위 글은 최신 쪽에서 다시 나오지 않는다")
    void stories_reactionThenLatest_noDuplicates() {
        Board hot1 = board(me, null);
        Board hot2 = board(me, null);
        List<Board> latest = new ArrayList<>(List.of(board(me, null), hot1, board(me, null), board(me, null), board(me, null)));
        givenStoryQueries(List.of(hot1, hot2), latest);
        given(competitionQueryService.getCards(any())).willReturn(Map.of());

        assertThat(boardService.getCompetitionStories(7L)).hasSize(5);

        List<Board> mapped = mappedBoards();
        assertThat(mapped).containsExactly(hot1, hot2, latest.get(0), latest.get(2), latest.get(3));
        verify(boardRepository).findCompetitionStoriesByReaction(eq(7L), any(), any(), eq(PageRequest.of(0, 2)));
        verify(boardRepository).findCompetitionStoriesLatest(eq(7L), any(), any(), eq(PageRequest.of(0, 5)));
    }

    @Test
    @DisplayName("대회 이야기: 반응이 없으면 최신순, 글이 적으면 있는 만큼, 없으면 빈 목록")
    void stories_latestOnly_andFewer() {
        Board a = board(me, null);
        Board b = board(me, null);
        givenStoryQueries(List.of(), List.of(a, b));
        given(competitionQueryService.getCards(any())).willReturn(Map.of());

        assertThat(boardService.getCompetitionStories(7L)).hasSize(2);
        assertThat(mappedBoards()).containsExactly(a, b);

        givenStoryQueries(List.of(), List.of());
        assertThat(boardService.getCompetitionStories(7L)).isEmpty();
    }

    @Test
    @DisplayName("대회 이야기: 피드와 같은 제외 목록(차단 작성자, 신고·숨긴 글)을 두 쿼리에 넘긴다")
    void stories_feedExclusions() {
        given(userBlockService.getBlockedUserIds()).willReturn(List.of(5L));
        given(reportService.getReportedTargetIds(ReportType.BOARD)).willReturn(List.of(300L));
        given(boardHideService.getHiddenContentIds()).willReturn(List.of(301L, 300L));
        given(boardRepository.findCompetitionStoriesByReaction(any(), any(), any(), any())).willReturn(List.of());
        given(boardRepository.findCompetitionStoriesLatest(any(), any(), any(), any())).willReturn(List.of());

        boardService.getCompetitionStories(7L);

        verify(boardRepository).findCompetitionStoriesByReaction(7L, List.of(5L), List.of(300L, 301L), PageRequest.of(0, 2));
        verify(boardRepository).findCompetitionStoriesLatest(7L, List.of(5L), List.of(300L, 301L), PageRequest.of(0, 5));
    }

    @Test
    @DisplayName("대회 이야기: 없거나 미공개 대회면 COMPETITION_NOT_FOUND")
    void stories_unpublished_notFound() {
        given(competitionQueryService.getPublished(9L)).willThrow(new ErrorException(ErrorCode.COMPETITION_NOT_FOUND));

        assertThatThrownBy(() -> boardService.getCompetitionStories(9L))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
        verifyNoInteractions(boardRepository);
    }

    // ---------- helpers ----------

    private void givenStoryQueries(List<Board> byReaction, List<Board> latest) {
        given(boardRepository.findCompetitionStoriesByReaction(any(), any(), any(), any())).willReturn(byReaction);
        given(boardRepository.findCompetitionStoriesLatest(any(), any(), any(), any())).willReturn(latest);
    }

    // 응답으로 매핑된 글 (마지막 호출 묶음 기준, 순서 유지)
    private List<Board> mappedBoards() {
        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardMapper, atLeastOnce()).toBoardListResponse(captor.capture(), any(), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), any());
        List<Board> all = captor.getAllValues();
        clearInvocations(boardMapper);
        return all;
    }

    private BoardUpdateRequest updateRequest(Long competitionId) {
        return new BoardUpdateRequest(null, null, null, null, null, competitionId);
    }

    private Board myBoard(Competition competition) {
        given(authenticationFacade.getCurrentUser()).willReturn(me);
        return board(me, competition);
    }

    private static long nextId = 1000;

    // 연결 교체를 실제 엔티티로 검증하려고 Board 는 mock 이 아닌 실제 객체
    private Board board(User author, Competition competition) {
        Content content = mock(Content.class);
        lenient().when(content.getId()).thenReturn(nextId++);
        lenient().when(content.getImageFiles()).thenReturn(new ArrayList<>());
        Board board = Board.builder()
                .id(nextId++)
                .content(content)
                .competition(competition)
                .title("제목")
                .body("본문")
                .build();
        ReflectionTestUtils.setField(board, "createdBy", author);
        return board;
    }

    private static Competition competition(Long id) {
        return Competition.builder().id(id).name("대회" + id).published(true).build();
    }

    private static CompetitionListItem card(Long id) {
        return new CompetitionListItem(id, "대회" + id, LocalDate.of(2026, 12, 6), null, null, List.of(), null,
                null, false, null, null, null, 0, 0, false);
    }
}
