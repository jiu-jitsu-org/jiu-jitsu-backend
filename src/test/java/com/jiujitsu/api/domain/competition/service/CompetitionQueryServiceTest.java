package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.CompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionFeeResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.CompetitionListRequest;
import com.jiujitsu.api.domain.competition.dto.CompetitionListResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionFee;
import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionStatus;
import com.jiujitsu.api.domain.competition.entity.CompetitionUniform;
import com.jiujitsu.api.domain.competition.mapper.CompetitionMapper;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

// 커서 조립은 실제 쿼리 결과에 달려 있어 DB 와 함께 검증한다 (인증만 mock, 오늘은 고정)
@DataJpaTest
@Import({CompetitionQueryService.class, CompetitionInterestService.class, CompetitionMapper.class})
class CompetitionQueryServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    @TestConfiguration
    static class FixedClock {
        @Bean
        Clock clock() {
            return Clock.fixed(TODAY.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        }
    }

    @Autowired
    private CompetitionQueryService competitionQueryService;

    @Autowired
    private TestEntityManager em;

    @MockBean
    private AuthenticationFacade authenticationFacade;

    private User me;

    @BeforeEach
    void anonymousByDefault() {
        me = em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.empty());
    }

    private void login() {
        given(authenticationFacade.getCurrentUserOptional()).willReturn(Optional.of(me));
    }

    private Competition save(String name, LocalDate openDate) {
        return em.persist(Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(openDate.getYear())
                .published(true)
                .openDate(openDate)
                .build());
    }

    private CompetitionListRequest request(LocalDate date, String cursor, Integer size) {
        return new CompetitionListRequest(date, cursor, size, null, null, null, null);
    }

    private static List<String> names(CompetitionListResponse response) {
        return response.competitions().stream().map(CompetitionListItem::name).toList();
    }

    // 과거 2 · 오늘 2(같은 날) · 미래 3, 그리고 미공개 1
    private void seedTimeline() {
        save("과거1", TODAY.minusDays(30));
        save("과거2", TODAY.minusDays(1));
        save("오늘1", TODAY);
        save("오늘2", TODAY);
        save("미래1", TODAY.plusDays(7));
        save("미래2", TODAY.plusDays(27));
        save("미래3", TODAY.plusDays(60));
        em.persist(Competition.of("카탈로그", 2026));
        em.flush();
    }

    @Test
    @DisplayName("첫 페이지: 기준일(기본 오늘, 포함) 이후 size 개, 양쪽 커서, 응답 today")
    void firstPage() {
        seedTimeline();

        CompetitionListResponse response = competitionQueryService.getList(request(null, null, 3));

        assertThat(response.today()).isEqualTo(TODAY);
        assertThat(names(response)).containsExactly("오늘1", "오늘2", "미래1");
        assertThat(response.nextCursor()).isNotNull();
        assertThat(response.previousCursor()).isNotNull();
    }

    @Test
    @DisplayName("앞으로 끝까지 넘기면 기준일 이후가, 뒤로 끝까지 넘기면 그 이전이 중복·누락 없이 한 번씩 나온다")
    void walkBothWays() {
        seedTimeline();
        CompetitionListResponse first = competitionQueryService.getList(request(null, null, 2));

        List<String> forward = new ArrayList<>(names(first));
        String next = first.nextCursor();
        while (next != null) {
            CompetitionListResponse page = competitionQueryService.getList(request(null, next, 2));
            forward.addAll(names(page));
            next = page.nextCursor();
        }
        List<String> backward = new ArrayList<>();
        String previous = first.previousCursor();
        while (previous != null) {
            CompetitionListResponse page = competitionQueryService.getList(request(null, previous, 1));
            backward.addAll(0, names(page));   // 이전 페이지는 위에 붙인다
            previous = page.previousCursor();
        }

        assertThat(forward).containsExactly("오늘1", "오늘2", "미래1", "미래2", "미래3");
        assertThat(backward).containsExactly("과거1", "과거2");
    }

    @Test
    @DisplayName("이전 페이지도 오름차순이고, 그 페이지의 nextCursor 로 다시 내려오면 처음 페이지와 이어진다")
    void previousPage_ascendingAndReturns() {
        seedTimeline();
        CompetitionListResponse first = competitionQueryService.getList(request(null, null, 2));

        CompetitionListResponse past = competitionQueryService.getList(request(null, first.previousCursor(), 5));
        assertThat(names(past)).containsExactly("과거1", "과거2");
        assertThat(past.previousCursor()).isNull();

        CompetitionListResponse back = competitionQueryService.getList(request(null, past.nextCursor(), 2));
        assertThat(names(back)).containsExactly("오늘1", "오늘2");
    }

    @Test
    @DisplayName("기준일 이후 대회가 없어도 과거가 있으면 previousCursor, 아무것도 없으면 둘 다 null")
    void emptyPage_cursors() {
        save("과거", TODAY.minusDays(3));
        em.flush();

        CompetitionListResponse future = competitionQueryService.getList(request(null, null, 10));
        assertThat(future.competitions()).isEmpty();
        assertThat(future.nextCursor()).isNull();
        assertThat(names(competitionQueryService.getList(request(null, future.previousCursor(), 10)))).containsExactly("과거");

        CompetitionListResponse none = competitionQueryService.getList(new CompetitionListRequest(null, null, 10, null, null, null, "없는대회"));
        assertThat(none.competitions()).isEmpty();
        assertThat(none.previousCursor()).isNull();
        assertThat(none.nextCursor()).isNull();
    }

    @Test
    @DisplayName("date 로 기준일을 고르면 그날부터")
    void customDate() {
        seedTimeline();

        assertThat(names(competitionQueryService.getList(request(TODAY.minusDays(1), null, 2)))).containsExactly("과거2", "오늘1");
    }

    @Test
    @DisplayName("항목: 상태·종목·관심 수·내 관심 여부 (로그인), 비로그인은 interested=false")
    void items_statusAndInterest() {
        Competition open = em.persist(Competition.builder()
                .name("서울 오픈").searchName("서울오픈").competitionYear(2026).published(true)
                .openDate(TODAY.plusDays(20))
                .registrationStartDate(TODAY.minusDays(5)).earlyBirdDeadline(TODAY.plusDays(2)).registrationDeadline(TODAY.plusDays(10))
                .uniforms(new LinkedHashSet<>(List.of(CompetitionUniform.NO_GI, CompetitionUniform.GI)))
                .build());
        User other = em.persist(User.builder().snsProvider(SnsProvider.KAKAO).snsId(UUID.randomUUID().toString())
                .role(UserRole.USER).status(UserStatus.ACTIVE).build());
        em.persist(new CompetitionInterest(open, me));
        em.persist(new CompetitionInterest(open, other));
        em.flush();

        CompetitionListItem anonymous = competitionQueryService.getList(request(null, null, 10)).competitions().get(0);
        assertThat(anonymous.status()).isEqualTo(CompetitionStatus.OPEN);
        assertThat(anonymous.earlyBirdOpen()).isTrue();
        assertThat(anonymous.uniforms()).containsExactly(CompetitionUniform.GI, CompetitionUniform.NO_GI);
        assertThat(anonymous.interestCount()).isEqualTo(2);
        assertThat(anonymous.viewCount()).isZero();
        assertThat(anonymous.interested()).isFalse();

        login();
        assertThat(competitionQueryService.getList(request(null, null, 10)).competitions().get(0).interested()).isTrue();
    }

    @Test
    @DisplayName("interested=true: 로그인하면 내 관심 대회만, 비로그인은 LOGIN_NOT_ACCESS")
    void interestedOnly() {
        Competition mine = save("내 관심", TODAY.plusDays(1));
        save("남의 대회", TODAY.plusDays(2));
        em.persist(new CompetitionInterest(mine, me));
        em.flush();
        CompetitionListRequest interested = new CompetitionListRequest(null, null, null, null, null, true, null);

        assertThatThrownBy(() -> competitionQueryService.getList(interested))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.LOGIN_NOT_ACCESS);

        login();
        assertThat(names(competitionQueryService.getList(interested))).containsExactly("내 관심");
    }

    @Test
    @DisplayName("date 와 cursor 를 같이 보내면 WRONG_PARAMETER")
    void dateWithCursor() {
        String cursor = CompetitionCursor.startOf(TODAY).encode();

        assertThatThrownBy(() -> competitionQueryService.getList(request(TODAY, cursor, null)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
    }

    @Test
    @DisplayName("빈 cursor 는 없는 것으로 본다 (첫 페이지, date 와 함께여도 에러 아님)")
    void blankCursor_isAbsent() {
        seedTimeline();

        assertThat(names(competitionQueryService.getList(request(null, "", 2)))).containsExactly("오늘1", "오늘2");
        assertThat(names(competitionQueryService.getList(request(TODAY.minusDays(1), " ", 1)))).containsExactly("과거2");
    }

    @Test
    @DisplayName("status 필터는 기준일(date)이 아니라 오늘 기준이고, region 과 함께 다음 페이지에도 유지된다")
    void statusAndRegion_todayBased_keptAcrossPages() {
        // 10/1 기준이면 접수 중이었지만 오늘은 마감된 대회 — date 를 과거로 줘도 OPEN 에 나오면 안 된다
        em.persist(Competition.builder().name("서울 마감").searchName("서울마감").competitionYear(2026).published(true)
                .region(CompetitionRegion.SEOUL).openDate(TODAY.plusDays(10))
                .registrationStartDate(TODAY.minusDays(60)).registrationDeadline(TODAY.minusDays(1)).build());
        for (int i = 1; i <= 3; i++) {
            em.persist(Competition.builder().name("서울 접수중" + i).searchName("서울접수중" + i).competitionYear(2026).published(true)
                    .region(CompetitionRegion.SEOUL).openDate(TODAY.plusDays(10 + i))
                    .registrationStartDate(TODAY.minusDays(60)).registrationDeadline(TODAY.plusDays(5)).build());
        }
        em.persist(Competition.builder().name("부산 접수중").searchName("부산접수중").competitionYear(2026).published(true)
                .region(CompetitionRegion.BUSAN).openDate(TODAY.plusDays(11)).build());
        // 첫 페이지 커서 뒤쪽에도 걸러져야 할 대회를 둔다 — 커서로 넘어갈 때 필터가 빠지면 2페이지에 섞인다
        em.persist(Competition.builder().name("부산 접수중2").searchName("부산접수중2").competitionYear(2026).published(true)
                .region(CompetitionRegion.BUSAN).openDate(TODAY.plusDays(20)).build());
        em.persist(Competition.builder().name("서울 마감2").searchName("서울마감2").competitionYear(2026).published(true)
                .region(CompetitionRegion.SEOUL).openDate(TODAY.plusDays(21))
                .registrationStartDate(TODAY.minusDays(60)).registrationDeadline(TODAY.minusDays(1)).build());
        em.flush();
        List<CompetitionRegion> seoul = List.of(CompetitionRegion.SEOUL);
        List<CompetitionStatus> open = List.of(CompetitionStatus.OPEN);

        CompetitionListResponse first = competitionQueryService.getList(
                new CompetitionListRequest(LocalDate.of(2026, 10, 1), null, 2, seoul, open, null, null));
        CompetitionListResponse second = competitionQueryService.getList(
                new CompetitionListRequest(null, first.nextCursor(), 2, seoul, open, null, null));

        assertThat(names(first)).containsExactly("서울 접수중1", "서울 접수중2");
        assertThat(names(second)).containsExactly("서울 접수중3");
        assertThat(second.nextCursor()).isNull();
        assertThat(java.util.stream.Stream.concat(first.competitions().stream(), second.competitions().stream()).toList()).allSatisfy(item -> {
            assertThat(item.status()).isEqualTo(CompetitionStatus.OPEN);
            assertThat(item.region()).isEqualTo(CompetitionRegion.SEOUL);
        });
    }

    // --- 상세 ---

    private Competition saveDetailed() {
        Competition competition = em.persist(Competition.builder()
                .name("서울 오픈").searchName("서울오픈").competitionYear(2026).published(true)
                .region(CompetitionRegion.SEOUL).place("서울 송파").venue("올림픽홀")
                .openDate(TODAY.plusDays(20)).startTime(LocalTime.of(9, 0))
                .host("서울 협회").noticeUrl("https://example.com/notice")
                .registrationStartDate(TODAY.minusDays(5)).earlyBirdDeadline(TODAY.plusDays(2)).registrationDeadline(TODAY.plusDays(10))
                .fees(new ArrayList<>(List.of(new CompetitionFee("얼리버드", 60000, TODAY.plusDays(2)), new CompetitionFee("기본 참가비", 70000, null))))
                .feeNote("성인 1종목 기준").lastVerifiedDate(TODAY.minusDays(1))
                .uniforms(new LinkedHashSet<>(List.of(CompetitionUniform.NO_GI, CompetitionUniform.GI)))
                .build());
        em.flush();
        em.clear();
        return competition;
    }

    @Test
    @DisplayName("상세: 기본 정보·일정·참가비 단계(입력 순서)·기준일·상태·today 를 담는다")
    void detail_fields() {
        Long id = saveDetailed().getId();

        CompetitionDetailResponse detail = competitionQueryService.getDetail(id);

        assertThat(detail.today()).isEqualTo(TODAY);
        assertThat(detail.competitionId()).isEqualTo(id);
        assertThat(detail.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(detail.place()).isEqualTo("서울 송파");
        assertThat(detail.venue()).isEqualTo("올림픽홀");
        assertThat(detail.host()).isEqualTo("서울 협회");
        assertThat(detail.uniforms()).containsExactly(CompetitionUniform.GI, CompetitionUniform.NO_GI);
        assertThat(detail.status()).isEqualTo(CompetitionStatus.OPEN);
        assertThat(detail.earlyBirdOpen()).isTrue();
        assertThat(detail.earlyBirdDeadline()).isEqualTo(TODAY.plusDays(2));
        assertThat(detail.fees()).containsExactly(
                new CompetitionFeeResponse("얼리버드", 60000, TODAY.plusDays(2)),
                new CompetitionFeeResponse("기본 참가비", 70000, null));
        assertThat(detail.feeNote()).isEqualTo("성인 1종목 기준");
        assertThat(detail.lastVerifiedDate()).isEqualTo(TODAY.minusDays(1));
    }

    @Test
    @DisplayName("상세: 관심 수는 누구나, 내 관심 여부는 로그인했을 때만 true")
    void detail_interest() {
        Competition competition = saveDetailed();
        Competition managed = em.find(Competition.class, competition.getId());
        em.persist(new CompetitionInterest(managed, me));
        em.flush();

        CompetitionDetailResponse anonymous = competitionQueryService.getDetail(competition.getId());
        assertThat(anonymous.interestCount()).isEqualTo(1);
        assertThat(anonymous.interested()).isFalse();

        login();
        assertThat(competitionQueryService.getDetail(competition.getId()).interested()).isTrue();
    }

    @Test
    @DisplayName("상세: 없거나 미공개면 COMPETITION_NOT_FOUND, 상세를 조회해도 조회수는 오르지 않는다")
    void detail_notFound_andNoViewSideEffect() {
        Long id = saveDetailed().getId();
        Long catalog = em.persist(Competition.of("카탈로그", 2026)).getId();
        em.flush();

        assertThatThrownBy(() -> competitionQueryService.getDetail(catalog))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
        assertThatThrownBy(() -> competitionQueryService.getDetail(-1L))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);

        competitionQueryService.getDetail(id);
        competitionQueryService.getDetail(id);
        em.clear();
        assertThat(competitionQueryService.getDetail(id).viewCount()).isZero();
    }
}
