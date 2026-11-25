package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.community.balance_game.factory.BalanceGameFactory;
import com.jiujitsu.api.domain.community.balance_game.mapper.BalanceGameMapper;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameRepository;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameVoteRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

/**
 * 조회 응답 네 곳(오늘의 게임 · 상세 · 투표 · 내가 투표한 목록)에 판 타이틀이 실린다. 타이틀 없는 지난 판은 null (#289)
 */
@ExtendWith(MockitoExtension.class)
class BalanceGameServiceTitleTest {

    private static final Long CONTENT_ID = 100L;

    @InjectMocks
    private BalanceGameService balanceGameService;

    @Mock private BalanceGameRepository balanceGameRepository;
    @Mock private BalanceGameVoteRepository balanceGameVoteRepository;
    @Mock private BalanceGameFactory balanceGameFactory;
    @Spy private BalanceGameMapper balanceGameMapper = new BalanceGameMapper();
    @Mock private CommunityCommentsService communityCommentsService;
    @Mock private ContentService contentService;
    @Mock private NoticeService noticeService;
    @Mock private AuthenticationFacade authenticationFacade;

    private final User user = new User();

    @BeforeEach
    void setUp() {
        user.setId(7L);
    }

    private BalanceGame game(String title) {
        LocalDateTime now = LocalDateTime.now();
        return BalanceGame.builder()
                .id(10L)
                .content(Content.builder().id(CONTENT_ID).contentType(ContentType.BALANCE).build())
                .title(title)
                .optionAText("옵션A")
                .optionBText("옵션B")
                .startAt(now.minusHours(1))
                .endAt(now.plusHours(1))
                .gameDate(now.toLocalDate())
                .build();
    }

    @ParameterizedTest(name = "[{index}] title={0}")
    @NullSource
    @ValueSource(strings = "더 싫은 패배는?")
    @DisplayName("오늘의 게임")
    void current(String title) {
        given(balanceGameRepository.findOpen(any(), any())).willReturn(List.of(game(title)));

        assertThat(balanceGameService.getCurrent().title()).isEqualTo(title);
    }

    @ParameterizedTest(name = "[{index}] title={0}")
    @NullSource
    @ValueSource(strings = "더 싫은 패배는?")
    @DisplayName("상세")
    void detail(String title) {
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.of(game(title)));

        assertThat(balanceGameService.getDetail(CONTENT_ID).title()).isEqualTo(title);
    }

    @ParameterizedTest(name = "[{index}] title={0}")
    @NullSource
    @ValueSource(strings = "더 싫은 패배는?")
    @DisplayName("투표")
    void vote(String title) {
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameRepository.findByContentId(CONTENT_ID)).willReturn(Optional.of(game(title)));

        assertThat(balanceGameService.vote(CONTENT_ID, BalanceGameOption.A).title()).isEqualTo(title);
    }

    @ParameterizedTest(name = "[{index}] title={0}")
    @NullSource
    @ValueSource(strings = "더 싫은 패배는?")
    @DisplayName("내가 투표한 목록")
    void participated(String title) {
        BalanceGameVote vote = BalanceGameVote.builder().balanceGame(game(title)).option(BalanceGameOption.A).build();
        given(authenticationFacade.getCurrentUser()).willReturn(user);
        given(balanceGameVoteRepository.findMyVotes(eq(user), any())).willReturn(new PageImpl<>(List.of(vote)));

        assertThat(balanceGameService.getParticipatedList(PageRequest.of(0, 20)).getContent().get(0).title()).isEqualTo(title);
    }
}
