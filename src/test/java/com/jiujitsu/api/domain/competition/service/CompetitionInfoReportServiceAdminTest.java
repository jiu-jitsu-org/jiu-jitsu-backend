package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionInfoReportListRequest;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionInfoReportResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionInfoReportResolveRequest;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionInfoField;
import com.jiujitsu.api.domain.competition.entity.CompetitionInfoReport;
import com.jiujitsu.api.domain.competition.mapper.CompetitionInfoReportMapper;
import com.jiujitsu.api.domain.competition.repository.CompetitionInfoReportRepository;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

// 관리자 목록 조건 조립(필터 · 정렬) · 처리 저장은 실제 쿼리 결과와 변경 감지에 달려 있어 DB 와 함께 검증한다 (AdminCompetitionServiceTest 와 같은 방식)
// 제보 등록 · 관리자 응답 매핑 · 없는 제보는 CompetitionInfoReportServiceTest (Mockito)
@DataJpaTest
@Import({CompetitionInfoReportService.class, CompetitionInfoReportMapper.class})
class CompetitionInfoReportServiceAdminTest {

    private static final LocalDateTime NOW = LocalDate.of(2026, 11, 13).atTime(15, 0);
    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20, Sort.by("detail"));   // sort 는 무시돼야 함

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(NOW.atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    private CompetitionInfoReportService competitionInfoReportService;

    @Autowired
    private CompetitionInfoReportRepository competitionInfoReportRepository;

    @Autowired
    private TestEntityManager em;

    @MockBean   // 제보 등록 의존성. 관리자 기능은 쓰지 않는다
    private AuthenticationFacade authenticationFacade;

    private User saveUser() {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .nickname("제보자")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private Competition saveCompetition(String name) {
        return em.persist(Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(2026)
                .published(true)
                .openDate(LocalDate.of(2026, 12, 6))
                .build());
    }

    // 제보 시각을 정해 저장 — 감사(@CreatedDate) 값과 상관없이 SQL 로 덮어쓴다
    private Long report(Competition competition, User user, LocalDateTime createdAt) {
        CompetitionInfoReport report = em.persistAndFlush(CompetitionInfoReport.of(competition, user, CompetitionInfoField.FEE, "내용"));
        em.getEntityManager().createNativeQuery("UPDATE competition_info_reports SET created_at = :at WHERE id = :id")
                .setParameter("at", createdAt)
                .setParameter("id", report.getId())
                .executeUpdate();
        return report.getId();
    }

    private void flushAndClear() {
        em.flush();
        em.clear();
    }

    private Long[] ids(AdminCompetitionInfoReportListRequest request) {
        return competitionInfoReportService.getAdminReports(request, FIRST_PAGE).getContent().stream()
                .map(AdminCompetitionInfoReportResponse::reportId)
                .toArray(Long[]::new);
    }

    @Test
    @DisplayName("관리자 목록: resolved · competitionId 를 조건으로 옮기고, 미처리 먼저 → 최신순 (sort 무시)")
    void getAdminReports_filters() {
        User user = saveUser();
        Competition seoul = saveCompetition("서울 오픈");
        Competition busan = saveCompetition("부산 오픈");
        Long seoulDone = report(seoul, user, NOW.minusHours(1));      // 가장 최신이지만 처리됨 → 맨 뒤
        Long seoulOpen = report(seoul, user, NOW.minusHours(3));
        Long busanOpen = report(busan, user, NOW.minusHours(2));
        competitionInfoReportService.resolve(seoulDone, null);
        flushAndClear();

        assertThat(ids(new AdminCompetitionInfoReportListRequest(null, null))).containsExactly(busanOpen, seoulOpen, seoulDone);
        assertThat(ids(new AdminCompetitionInfoReportListRequest(false, null))).containsExactly(busanOpen, seoulOpen);
        assertThat(ids(new AdminCompetitionInfoReportListRequest(true, null))).containsExactly(seoulDone);
        assertThat(ids(new AdminCompetitionInfoReportListRequest(null, seoul.getId()))).containsExactly(seoulOpen, seoulDone);
        assertThat(ids(new AdminCompetitionInfoReportListRequest(false, busan.getId()))).containsExactly(busanOpen);
        assertThat(ids(new AdminCompetitionInfoReportListRequest(true, busan.getId()))).isEmpty();
    }

    @Test
    @DisplayName("처리 · 되돌리기는 DB 에 남는다: 처리 시각 = 지금, 메모 없이 다시 처리하면 시각 · 메모 그대로, 빈 메모는 지움, 되돌리면 둘 다 지움")
    void resolve_persists() {
        Long id = report(saveCompetition("서울 오픈"), saveUser(), NOW.minusDays(1));
        flushAndClear();

        competitionInfoReportService.resolve(id, new CompetitionInfoReportResolveRequest(" 참가비 고침 "));
        flushAndClear();
        CompetitionInfoReport saved = competitionInfoReportRepository.findById(id).orElseThrow();
        assertThat(saved.getResolvedAt()).isEqualTo(NOW);
        assertThat(saved.getResolutionNote()).isEqualTo("참가비 고침");

        competitionInfoReportService.resolve(id, null);
        flushAndClear();
        saved = competitionInfoReportRepository.findById(id).orElseThrow();
        assertThat(saved.getResolvedAt()).isEqualTo(NOW);
        assertThat(saved.getResolutionNote()).isEqualTo("참가비 고침");

        competitionInfoReportService.resolve(id, new CompetitionInfoReportResolveRequest(""));
        flushAndClear();
        assertThat(competitionInfoReportRepository.findById(id).orElseThrow().getResolutionNote()).isNull();

        competitionInfoReportService.unresolve(id);
        flushAndClear();
        saved = competitionInfoReportRepository.findById(id).orElseThrow();
        assertThat(saved.getResolvedAt()).isNull();
        assertThat(saved.getResolutionNote()).isNull();
    }
}
