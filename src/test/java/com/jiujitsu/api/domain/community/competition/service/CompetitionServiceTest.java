package com.jiujitsu.api.domain.community.competition.service;

import com.jiujitsu.api.domain.community.competition.dto.CompetitionCreateRequest;
import com.jiujitsu.api.domain.community.competition.dto.CompetitionCreateResponse;
import com.jiujitsu.api.domain.community.competition.dto.CompetitionCreateResponse.SkipReason;
import com.jiujitsu.api.domain.community.competition.dto.CompetitionResponse;
import com.jiujitsu.api.domain.community.competition.entity.Competition;
import com.jiujitsu.api.domain.community.competition.mapper.CompetitionMapper;
import com.jiujitsu.api.domain.community.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompetitionServiceTest {

    @InjectMocks
    private CompetitionService competitionService;

    @Mock private CompetitionRepository competitionRepository;
    @Mock private AuthenticationFacade authenticationFacade;
    @Spy private CompetitionMapper competitionMapper = new CompetitionMapper();

    @Test
    @DisplayName("검색어를 공백 제거·소문자로 정규화하고 LIKE 특수문자를 이스케이프해 20개까지 조회한다")
    void search_normalizesKeyword() {
        given(competitionRepository.search(anyString(), anyString(), anyString(), any(Pageable.class)))
                .willReturn(List.of(Competition.of("Seoul Open 100%", 2026)));

        List<CompetitionResponse> result = competitionService.search("  SEOUL open 100% ");

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(competitionRepository).search(eq("seoulopen100%"), eq("seoulopen100\\%%"), eq("%seoulopen100\\%%"), pageable.capture());
        assertThat(pageable.getValue().getPageSize()).isEqualTo(CompetitionService.SEARCH_LIMIT);
        assertThat(result).extracting(CompetitionResponse::name, CompetitionResponse::year)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("Seoul Open 100%", 2026));
    }

    @Test
    @DisplayName("검색어가 비었거나 공백뿐이거나 대회명 최대 길이보다 길면 조회 없이 빈 배열을 반환한다")
    void search_blankKeyword_returnsEmpty() {
        assertThat(competitionService.search(null)).isEmpty();
        assertThat(competitionService.search(" \u00A0 ")).isEmpty();
        assertThat(competitionService.search("가".repeat(Competition.NAME_MAX_LENGTH + 1))).isEmpty();

        verify(competitionRepository, never()).search(anyString(), anyString(), anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("로그인하지 않으면 LOGIN_NOT_ACCESS")
    void search_requiresLogin() {
        willThrow(new ErrorException(ErrorCode.LOGIN_NOT_ACCESS)).given(authenticationFacade).checkCurrentUser();

        assertThatThrownBy(() -> competitionService.search("서울"))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_NOT_ACCESS);
        verify(competitionRepository, never()).search(anyString(), anyString(), anyString(), any(Pageable.class));
    }

    @Test
    @DisplayName("이름(공백·대소문자 무시)과 연도가 같은 대회만 건너뛰고, 연도가 다르면 따로 등록한다")
    @SuppressWarnings("unchecked")
    void create_skipsDuplicates() {
        given(competitionRepository.findAllBySearchNameIn(anyCollection()))
                .willReturn(List.of(Competition.of("부산 오픈", 2026), Competition.of("대구 오픈", null)));
        given(competitionRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        CompetitionCreateRequest request = new CompetitionCreateRequest(List.of(
                new CompetitionCreateRequest.Item(" 서울 오픈 ", 2026),
                new CompetitionCreateRequest.Item("서울오픈", 2026),      // 요청 안 중복
                new CompetitionCreateRequest.Item("서울 오픈", 2025),     // 연도 다름 → 등록
                new CompetitionCreateRequest.Item("부산오픈", 2026),      // DB 중복
                new CompetitionCreateRequest.Item("부산 오픈", 2025),     // DB 와 연도 다름 → 등록
                new CompetitionCreateRequest.Item("대구오픈", null),      // DB 에 연도 없는 같은 이름
                new CompetitionCreateRequest.Item("광주 오픈", null),
                new CompetitionCreateRequest.Item("광주오픈", null)       // 요청 안 연도 없는 중복
        ));

        CompetitionCreateResponse response = competitionService.create(request);

        ArgumentCaptor<List<Competition>> saved = ArgumentCaptor.forClass(List.class);
        verify(competitionRepository).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(Competition::getName, Competition::getCompetitionYear)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("서울 오픈", 2026),
                        org.assertj.core.groups.Tuple.tuple("서울 오픈", 2025),
                        org.assertj.core.groups.Tuple.tuple("부산 오픈", 2025),
                        org.assertj.core.groups.Tuple.tuple("광주 오픈", null));
        assertThat(response.created()).hasSize(4);
        assertThat(response.skipped()).containsExactly(
                new CompetitionCreateResponse.Skipped("서울오픈", 2026, SkipReason.DUPLICATE),
                new CompetitionCreateResponse.Skipped("부산오픈", 2026, SkipReason.DUPLICATE),
                new CompetitionCreateResponse.Skipped("대구오픈", null, SkipReason.DUPLICATE),
                new CompetitionCreateResponse.Skipped("광주오픈", null, SkipReason.DUPLICATE));
    }

    @Test
    @DisplayName("정리하면 비는 이름(NBSP·ZWSP 만 있는 이름)은 INVALID_NAME 으로 건너뛰고 나머지는 등록한다")
    @SuppressWarnings("unchecked")
    void create_invalidName_skippedWithReason() {
        given(competitionRepository.findAllBySearchNameIn(anyCollection())).willReturn(List.of());
        given(competitionRepository.saveAll(anyList())).willAnswer(invocation -> invocation.getArgument(0));

        CompetitionCreateRequest request = new CompetitionCreateRequest(List.of(
                new CompetitionCreateRequest.Item("서울 오픈", 2026),
                new CompetitionCreateRequest.Item("\u00A0\u200B", 2026)
        ));

        CompetitionCreateResponse response = competitionService.create(request);

        assertThat(response.created()).extracting(CompetitionResponse::name).containsExactly("서울 오픈");
        assertThat(response.skipped()).containsExactly(
                new CompetitionCreateResponse.Skipped("\u00A0\u200B", 2026, SkipReason.INVALID_NAME));
    }
}
