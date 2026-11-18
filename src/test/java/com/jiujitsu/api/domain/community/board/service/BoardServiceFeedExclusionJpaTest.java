package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.BoardListRequest;
import com.jiujitsu.api.domain.community.board.dto.CompetitionStoriesResponse;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.board.entity.BoardListType;
import com.jiujitsu.api.domain.community.board.factory.BoardFactory;
import com.jiujitsu.api.domain.community.board.mapper.BoardMapper;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.service.CompetitionQueryService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verify;

/**
 * 서비스가 만든 조건을 실제 DB(H2)에서 — 피드 제외(차단 · 신고 · 숨김 합침) + 대회 조건이
 * 대회 이야기(#270)와 FEED + competitionId 목록(#269)에서 같은 글 · 같은 수로 나오는지.
 * 리포지토리만 실제, 나머지는 목 (@Mock · @InjectMocks 는 Spring 테스트 리스너가 초기화 — MockitoExtension 을 같이 쓰면 두 번 만들어져 stub 이 서비스에 안 닿음)
 */
@DataJpaTest
class BoardServiceFeedExclusionJpaTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 11, 1, 12, 0);

    @Autowired private BoardRepository boardRepository;
    @Autowired private TestEntityManager em;

    @InjectMocks private BoardService boardService;

    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private UserRepository userRepository;
    @Mock private BoardRepository mockBoardRepository;
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

    private User author;
    private BoardCategory category;
    private Competition seoul;
    private int minutes;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(boardService, "boardRepository", boardRepository);   // 리포지토리만 실제
        author = saveUser();
        category = em.persist(BoardCategory.builder().name("대회").isActive(true).build());
        seoul = competition("서울 오픈");
    }

    @Test
    @DisplayName("차단한 작성자 · 신고한 글 · 숨긴 글(신고와 겹쳐도) · 다른 대회 글은 대회 이야기와 FEED + competitionId 목록 모두에서 빠지고, 수가 같다")
    void storiesAndFeedFilter_sameExclusions() {
        User blocked = saveUser();
        post("보임1", seoul, author);
        post("차단작성자", seoul, blocked);
        Board reported = post("신고한글", seoul, author);
        Board hiddenByMe = post("내가숨김", seoul, author);
        post("다른대회", competition("광주 오픈"), author);
        post("보임2", seoul, author);
        em.flush();
        em.clear();

        given(userBlockService.getBlockedUserIds()).willReturn(List.of(blocked.getId()));
        given(reportService.getReportedTargetIds(ReportType.BOARD)).willReturn(List.of(reported.getContent().getId()));
        given(boardHideService.getHiddenContentIds()).willReturn(List.of(hiddenByMe.getContent().getId(), reported.getContent().getId()));

        CompetitionStoriesResponse stories = boardService.getCompetitionStories(seoul.getId());
        assertThat(stories.totalCount()).isEqualTo(2);
        assertThat(mappedTitles()).containsExactly("보임2", "보임1");

        long feedTotal = boardService.getList(new BoardListRequest(BoardListType.FEED, null, null, seoul.getId()),
                PageRequest.of(0, 20, Sort.by(Sort.Direction.DESC, "createdAt"))).getTotalElements();
        assertThat(feedTotal).isEqualTo(stories.totalCount());
        assertThat(mappedTitles()).containsExactly("보임2", "보임1");
    }

    @Test
    @DisplayName("제외할 게 없으면(빈 목록 → sentinel) 이 대회 글 전부")
    void noExclusions() {
        post("하나", seoul, author);
        post("둘", seoul, author);
        em.flush();
        em.clear();

        assertThat(boardService.getCompetitionStories(seoul.getId()).totalCount()).isEqualTo(2);
    }

    private List<String> mappedTitles() {
        ArgumentCaptor<Board> captor = ArgumentCaptor.forClass(Board.class);
        verify(boardMapper, atLeastOnce()).toBoardListResponse(captor.capture(), any(), anyLong(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), anyBoolean(), anyBoolean(), any());
        List<String> titles = captor.getAllValues().stream().map(Board::getTitle).toList();
        clearInvocations(boardMapper);
        return titles;
    }

    private User saveUser() {
        return em.persist(User.builder().snsProvider(SnsProvider.KAKAO).snsId(UUID.randomUUID().toString())
                .role(UserRole.USER).status(UserStatus.ACTIVE).build());
    }

    private Competition competition(String name) {
        return em.persist(Competition.builder().name(name).searchName(Competition.normalize(name))
                .competitionYear(2026).published(true).openDate(LocalDate.of(2026, 12, 6)).build());
    }

    // 부를 때마다 1분씩 늦게 쓴 글 (작성 시각을 고정해 최신순을 확정한다)
    private Board post(String title, Competition linked, User writer) {
        Board board = Board.builder().category(category)
                .content(Content.builder().contentType(ContentType.BOARD).build())
                .competition(linked).title(title).body("").build();
        ReflectionTestUtils.setField(board, "createdBy", writer);
        em.persist(board);
        em.flush();
        em.getEntityManager().createNativeQuery("UPDATE board SET created_at = ?1 WHERE id = ?2")
                .setParameter(1, BASE.plusMinutes(minutes++))
                .setParameter(2, board.getId())
                .executeUpdate();
        return board;
    }
}
