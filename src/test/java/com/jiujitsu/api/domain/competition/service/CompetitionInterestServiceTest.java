package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.CompetitionInterestResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import com.jiujitsu.api.domain.competition.repository.CompetitionInterestRepository;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompetitionInterestServiceTest {

    private static final Long COMPETITION_ID = 10L;

    @InjectMocks private CompetitionInterestService competitionInterestService;

    @Mock private CompetitionInterestRepository competitionInterestRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private AuthenticationFacade authenticationFacade;

    private final User user = User.builder().id(1L).role(UserRole.USER).build();

    private final Competition competition = Competition.builder()
            .id(COMPETITION_ID)
            .name("서울 오픈")
            .searchName("서울오픈")
            .published(true)
            .openDate(LocalDate.of(2026, 12, 6))
            .build();

    private void givenLoginAndPublished() {
        given(authenticationFacade.getCurrentUserForUpdate()).willReturn(user);
        given(competitionRepository.findByIdAndPublishedTrue(COMPETITION_ID)).willReturn(Optional.of(competition));
    }

    @Test
    @DisplayName("관심 등록: 없으면 저장하고, 처리 후 상태와 관심 수를 돌려준다")
    void register_creates() {
        givenLoginAndPublished();
        given(competitionInterestRepository.existsByCompetitionIdAndUserId(COMPETITION_ID, 1L)).willReturn(false);
        given(competitionInterestRepository.countByCompetitionId(COMPETITION_ID)).willReturn(3L);

        CompetitionInterestResponse response = competitionInterestService.register(COMPETITION_ID);

        ArgumentCaptor<CompetitionInterest> saved = ArgumentCaptor.forClass(CompetitionInterest.class);
        verify(competitionInterestRepository).save(saved.capture());
        assertThat(saved.getValue().getCompetition()).isSameAs(competition);
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(response).isEqualTo(new CompetitionInterestResponse(COMPETITION_ID, true, 3L));
    }

    @Test
    @DisplayName("관심 등록: 이미 등록돼 있으면 다시 저장하지 않고 같은 결과를 돌려준다 (멱등)")
    void register_idempotent() {
        givenLoginAndPublished();
        given(competitionInterestRepository.existsByCompetitionIdAndUserId(COMPETITION_ID, 1L)).willReturn(true);
        given(competitionInterestRepository.countByCompetitionId(COMPETITION_ID)).willReturn(3L);

        CompetitionInterestResponse response = competitionInterestService.register(COMPETITION_ID);

        verify(competitionInterestRepository, never()).save(any());
        assertThat(response).isEqualTo(new CompetitionInterestResponse(COMPETITION_ID, true, 3L));
    }

    @Test
    @DisplayName("관심 해제: 삭제하고, 처리 후 상태와 관심 수를 돌려준다")
    void unregister_deletes() {
        givenLoginAndPublished();
        given(competitionInterestRepository.countByCompetitionId(COMPETITION_ID)).willReturn(2L);

        CompetitionInterestResponse response = competitionInterestService.unregister(COMPETITION_ID);

        verify(competitionInterestRepository).deleteByCompetitionIdAndUserId(COMPETITION_ID, 1L);
        assertThat(response).isEqualTo(new CompetitionInterestResponse(COMPETITION_ID, false, 2L));
    }

    @Test
    @DisplayName("관심 해제: 등록돼 있지 않아도 에러 없이 해제 상태를 돌려준다 (멱등)")
    void unregister_idempotent() {
        givenLoginAndPublished();
        given(competitionInterestRepository.countByCompetitionId(COMPETITION_ID)).willReturn(0L);

        CompetitionInterestResponse response = competitionInterestService.unregister(COMPETITION_ID);

        assertThat(response).isEqualTo(new CompetitionInterestResponse(COMPETITION_ID, false, 0L));
    }

    @Test
    @DisplayName("없거나 미공개인 대회는 COMPETITION_NOT_FOUND (등록·해제 모두)")
    void notPublished_notFound() {
        given(authenticationFacade.getCurrentUserForUpdate()).willReturn(user);
        given(competitionRepository.findByIdAndPublishedTrue(anyLong())).willReturn(Optional.empty());

        assertThatThrownBy(() -> competitionInterestService.register(COMPETITION_ID))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
        assertThatThrownBy(() -> competitionInterestService.unregister(COMPETITION_ID))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
        verify(competitionInterestRepository, never()).save(any());
        verify(competitionInterestRepository, never()).deleteByCompetitionIdAndUserId(anyLong(), anyLong());
    }

    @Test
    @DisplayName("비로그인은 LOGIN_NOT_ACCESS")
    void anonymous_loginNotAccess() {
        given(authenticationFacade.getCurrentUserForUpdate()).willThrow(new ErrorException(ErrorCode.LOGIN_NOT_ACCESS));

        assertThatThrownBy(() -> competitionInterestService.register(COMPETITION_ID))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_NOT_ACCESS);
        verify(competitionInterestRepository, never()).save(any());
    }

    @Test
    @DisplayName("관심 수 일괄 조회: 집계 행을 대회 id → 수 맵으로, 빈 목록이면 조회하지 않는다")
    void getInterestCounts() {
        given(competitionInterestRepository.countByCompetitionIds(List.of(1L, 2L)))
                .willReturn(List.<Object[]>of(new Object[]{1L, 5L}));

        assertThat(competitionInterestService.getInterestCounts(List.of(1L, 2L))).containsExactly(Map.entry(1L, 5L));
        assertThat(competitionInterestService.getInterestCounts(List.of())).isEmpty();
        verify(competitionInterestRepository).countByCompetitionIds(any());
    }
}
