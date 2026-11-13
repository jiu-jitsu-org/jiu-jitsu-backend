package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionInfoReportListRequest;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionInfoReportResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionInfoReportRequest;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionInfoField;
import com.jiujitsu.api.domain.competition.entity.CompetitionInfoReport;
import com.jiujitsu.api.domain.competition.mapper.CompetitionInfoReportMapper;
import com.jiujitsu.api.domain.competition.repository.CompetitionInfoReportRepository;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CompetitionInfoReportServiceTest {

    private static final Long COMPETITION_ID = 10L;
    private static final LocalDate TODAY = LocalDate.of(2026, 11, 11);
    // 오후 3시 — 하루 기준이 "지금부터 24시간"이 아니라 오늘 자정인지 보기 위해 자정이 아닌 시각으로 고정
    private static final Clock CLOCK = Clock.fixed(TODAY.atTime(15, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));

    private CompetitionInfoReportService competitionInfoReportService;

    @Mock private CompetitionInfoReportRepository competitionInfoReportRepository;
    @Mock private CompetitionRepository competitionRepository;
    @Mock private AuthenticationFacade authenticationFacade;
    @Spy private CompetitionInfoReportMapper competitionInfoReportMapper = new CompetitionInfoReportMapper();

    private final User user = User.builder().id(1L).nickname("주짓수러").role(UserRole.USER).build();

    private final Competition competition = Competition.builder()
            .id(COMPETITION_ID)
            .name("서울 오픈")
            .searchName("서울오픈")
            .competitionYear(2026)
            .published(true)
            .openDate(LocalDate.of(2026, 12, 6))
            .build();

    // Clock 은 목이 아닌 고정 시계를 넣어야 해서 @InjectMocks 대신 직접 만든다
    @BeforeEach
    void setUp() {
        competitionInfoReportService = new CompetitionInfoReportService(
                competitionInfoReportRepository, competitionRepository, authenticationFacade, competitionInfoReportMapper, CLOCK);
    }

    private void givenLoginAndPublished(Competition target) {
        given(authenticationFacade.getCurrentUserForUpdate()).willReturn(user);
        given(competitionRepository.findByIdAndPublishedTrue(COMPETITION_ID)).willReturn(Optional.of(target));
    }

    @Test
    @DisplayName("제보 등록: 공개 대회면 항목·내용·제보자·대회를 저장하고, 오늘 자정 이후 같은 항목 수로 횟수를 센다")
    void report_saves() {
        givenLoginAndPublished(competition);
        given(competitionInfoReportRepository.countSince(COMPETITION_ID, 1L, CompetitionInfoField.REGISTRATION, TODAY.atStartOfDay()))
                .willReturn(2L);

        competitionInfoReportService.report(COMPETITION_ID, new CompetitionInfoReportRequest(CompetitionInfoField.REGISTRATION, " 마감 11.3 "));

        ArgumentCaptor<CompetitionInfoReport> saved = ArgumentCaptor.forClass(CompetitionInfoReport.class);
        verify(competitionInfoReportRepository).save(saved.capture());
        assertThat(saved.getValue().getCompetition()).isSameAs(competition);
        assertThat(saved.getValue().getUser()).isSameAs(user);
        assertThat(saved.getValue().getField()).isEqualTo(CompetitionInfoField.REGISTRATION);
        assertThat(saved.getValue().getDetail()).isEqualTo("마감 11.3");
    }

    @Test
    @DisplayName("제보 등록: 끝난 공개 대회도 받는다")
    void report_finishedCompetition() {
        Competition finished = Competition.builder()
                .id(COMPETITION_ID).name("지난 대회").searchName("지난대회").published(true)
                .openDate(TODAY.minusMonths(1))
                .build();
        givenLoginAndPublished(finished);

        competitionInfoReportService.report(COMPETITION_ID, new CompetitionInfoReportRequest(CompetitionInfoField.PLACE, null));

        verify(competitionInfoReportRepository).save(any(CompetitionInfoReport.class));
    }

    @Test
    @DisplayName("제보 등록: 오늘 같은 항목을 이미 3번 제보했으면 LIMIT_EXCEEDED, 저장하지 않는다")
    void report_limitExceeded() {
        givenLoginAndPublished(competition);
        given(competitionInfoReportRepository.countSince(COMPETITION_ID, 1L, CompetitionInfoField.FEE, TODAY.atStartOfDay()))
                .willReturn(3L);

        assertThatThrownBy(() -> competitionInfoReportService.report(COMPETITION_ID, new CompetitionInfoReportRequest(CompetitionInfoField.FEE, null)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_INFO_REPORT_LIMIT_EXCEEDED);
        verify(competitionInfoReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("제보 등록: 없거나 미공개인 대회는 COMPETITION_NOT_FOUND, 저장하지 않는다")
    void report_notPublished() {
        given(authenticationFacade.getCurrentUserForUpdate()).willReturn(user);
        given(competitionRepository.findByIdAndPublishedTrue(COMPETITION_ID)).willReturn(Optional.empty());

        assertThatThrownBy(() -> competitionInfoReportService.report(COMPETITION_ID, new CompetitionInfoReportRequest(CompetitionInfoField.FEE, null)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
        verify(competitionInfoReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("제보 등록: 비로그인이면 로그인 예외, 대회 조회·저장하지 않는다")
    void report_requiresLogin() {
        given(authenticationFacade.getCurrentUserForUpdate()).willThrow(new ErrorException(ErrorCode.LOGIN_NOT_ACCESS));

        assertThatThrownBy(() -> competitionInfoReportService.report(COMPETITION_ID, new CompetitionInfoReportRequest(CompetitionInfoField.FEE, null)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_NOT_ACCESS);
        verify(competitionRepository, never()).findByIdAndPublishedTrue(anyLong());
        verify(competitionInfoReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("제보 등록: OTHER 인데 내용이 없으면 WRONG_PARAMETER, 횟수 조회·저장하지 않는다")
    void report_otherWithoutDetail() {
        givenLoginAndPublished(competition);

        assertThatThrownBy(() -> competitionInfoReportService.report(COMPETITION_ID, new CompetitionInfoReportRequest(CompetitionInfoField.OTHER, " ")))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
        verify(competitionInfoReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("관리자 목록: 클라이언트 sort 는 버리고 page·size 만 넘기며, 대회·제보자 정보를 담아 돌려준다")
    void getAdminReports() {
        CompetitionInfoReport report = CompetitionInfoReport.of(competition, user, CompetitionInfoField.FEE, "7만 원이 아니라 8만 원");
        given(competitionInfoReportRepository.findAll(any(Specification.class), eq(PageRequest.of(1, 5))))
                .willReturn(new PageImpl<>(List.of(report), PageRequest.of(1, 5), 6));

        Pageable requested = PageRequest.of(1, 5, Sort.by("detail"));
        Page<AdminCompetitionInfoReportResponse> page = competitionInfoReportService.getAdminReports(
                new AdminCompetitionInfoReportListRequest(null, null), requested);

        assertThat(page.getTotalElements()).isEqualTo(6);
        AdminCompetitionInfoReportResponse item = page.getContent().get(0);
        assertThat(item.competitionId()).isEqualTo(COMPETITION_ID);
        assertThat(item.competitionName()).isEqualTo("서울 오픈");
        assertThat(item.competitionYear()).isEqualTo(2026);
        assertThat(item.field()).isEqualTo(CompetitionInfoField.FEE);
        assertThat(item.detail()).isEqualTo("7만 원이 아니라 8만 원");
        assertThat(item.reporterUserId()).isEqualTo(1L);
        assertThat(item.reporterNickname()).isEqualTo("주짓수러");
    }

    @Test
    @DisplayName("없는 제보 처리 · 되돌리기는 COMPETITION_INFO_REPORT_NOT_FOUND")
    void resolve_notFound() {
        given(competitionInfoReportRepository.findById(9L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> competitionInfoReportService.resolve(9L, null))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_INFO_REPORT_NOT_FOUND);
        assertThatThrownBy(() -> competitionInfoReportService.unresolve(9L))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_INFO_REPORT_NOT_FOUND);
    }
}
