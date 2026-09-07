package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameCreateRequest;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.factory.BalanceGameFactory;
import com.jiujitsu.api.domain.community.balance_game.mapper.BalanceGameMapper;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameRepository;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameVoteRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class BalanceGameServiceCreateTest {

    @InjectMocks
    private BalanceGameService balanceGameService;

    @Mock private BalanceGameRepository balanceGameRepository;
    @Mock private BalanceGameVoteRepository balanceGameVoteRepository;
    @Mock private BalanceGameFactory balanceGameFactory;
    @Mock private BalanceGameMapper balanceGameMapper;
    @Mock private CommunityCommentsService communityCommentsService;
    @Mock private ContentService contentService;
    @Mock private NoticeService noticeService;
    @Mock private AuthenticationFacade authenticationFacade;

    @Test
    @DisplayName("endAt 미입력 시 당일 23:59:59로 자동 설정된다")
    void create_withNullEndAt_defaultsToEndOfDay() {
        // given
        BalanceGameCreateRequest request = new BalanceGameCreateRequest(
                "옵션A", null, "옵션B", null, null);

        Content content = Content.builder().contentType(ContentType.BALANCE).build();
        BalanceGame game = BalanceGame.builder()
                .content(content).optionAText("옵션A").optionBText("옵션B")
                .endAt(LocalDateTime.now().plusHours(1))
                .build();

        given(balanceGameFactory.createContent()).willReturn(content);
        given(balanceGameFactory.createBalanceGame(any(), any(), any(), any(), any(), any())).willReturn(game);
        given(balanceGameRepository.save(any())).willReturn(game);
        given(balanceGameMapper.toResponse(any(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), any(), anyLong(), anyLong(), any(), any()))
                .willReturn(null);

        // when
        balanceGameService.create(request);

        // then - factory에 전달된 endAt이 23:59:59인지 확인
        ArgumentCaptor<LocalDateTime> endAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(balanceGameFactory).createBalanceGame(
                eq(content), eq("옵션A"), eq(null), eq("옵션B"), eq(null), endAtCaptor.capture());

        LocalDateTime capturedEndAt = endAtCaptor.getValue();
        assertThat(capturedEndAt.toLocalTime()).isEqualTo(LocalTime.of(23, 59, 59));
    }

    @Test
    @DisplayName("endAt 입력 시 해당 값이 그대로 사용된다")
    void create_withExplicitEndAt_usesProvidedValue() {
        // given
        LocalDateTime futureEndAt = LocalDateTime.now().plusDays(3);
        BalanceGameCreateRequest request = new BalanceGameCreateRequest(
                "옵션A", null, "옵션B", null, futureEndAt);

        Content content = Content.builder().contentType(ContentType.BALANCE).build();
        BalanceGame game = BalanceGame.builder()
                .content(content).optionAText("옵션A").optionBText("옵션B")
                .endAt(futureEndAt)
                .build();

        given(balanceGameFactory.createContent()).willReturn(content);
        given(balanceGameFactory.createBalanceGame(any(), any(), any(), any(), any(), any())).willReturn(game);
        given(balanceGameRepository.save(any())).willReturn(game);
        given(balanceGameMapper.toResponse(any(), anyLong(), anyLong(), anyBoolean(), anyBoolean(), any(), anyLong(), anyLong(), any(), any()))
                .willReturn(null);

        // when
        balanceGameService.create(request);

        // then
        ArgumentCaptor<LocalDateTime> endAtCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(balanceGameFactory).createBalanceGame(
                any(), any(), any(), any(), any(), endAtCaptor.capture());

        assertThat(endAtCaptor.getValue()).isEqualTo(futureEndAt);
    }

    @Test
    @DisplayName("endAt이 과거 시간이면 예외가 발생한다")
    void create_withPastEndAt_throwsException() {
        // given
        LocalDateTime pastEndAt = LocalDateTime.now().minusHours(1);
        BalanceGameCreateRequest request = new BalanceGameCreateRequest(
                "옵션A", null, "옵션B", null, pastEndAt);

        // when & then
        assertThatThrownBy(() -> balanceGameService.create(request))
                .isInstanceOf(ErrorException.class)
                .extracting(e -> ((ErrorException) e).getErrorCode())
                .isEqualTo(ErrorCode.BALANCE_GAME_END_AT_MUST_BE_FUTURE);
    }
}
