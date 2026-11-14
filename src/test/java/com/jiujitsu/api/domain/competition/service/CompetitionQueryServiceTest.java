package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.CompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionFeeResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionLinkResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.CompetitionListRequest;
import com.jiujitsu.api.domain.competition.dto.CompetitionListResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionPickerResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionRegionsResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionAudience;
import com.jiujitsu.api.domain.competition.entity.CompetitionFee;
import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import com.jiujitsu.api.domain.competition.entity.CompetitionLink;
import com.jiujitsu.api.domain.competition.entity.CompetitionLinkType;
import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionStatus;
import com.jiujitsu.api.domain.competition.entity.CompetitionUniform;
import com.jiujitsu.api.domain.competition.entity.RegistrationSite;
import com.jiujitsu.api.domain.competition.mapper.CompetitionMapper;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.properties.PopularCompetitionProperties;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
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
            // 낮 시각 — 상세 링크가 "오늘 00:00"이 아니라 지금 시각으로 걸러지는지 볼 수 있게 (#223)
            return Clock.fixed(TODAY.atTime(12, 0).atZone(ZoneId.of("Asia/Seoul")).toInstant(), ZoneId.of("Asia/Seoul"));
        }

        // 관심 많은 대회: 테스트는 상위 2개, 관심 2개 이상 (#210)
        @Bean
        PopularCompetitionProperties popularCompetitionProperties() {
            return new PopularCompetitionProperties(2, 2);
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

    private Competition saveIn(String name, CompetitionRegion region, LocalDate openDate) {
        return em.persist(Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(openDate.getYear())
                .published(true)
                .region(region)
                .openDate(openDate)
                .build());
    }

    // 여러 날 대회 (#223)
    private Competition saveMultiDay(String name, LocalDate openDate, LocalDate endDate) {
        return em.persist(Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(openDate.getYear())
                .published(true)
                .openDate(openDate)
                .endDate(endDate)
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

    @Test
    @DisplayName("여러 날 대회: 진행 중이면 status=CLOSED 필터에 나오고 ENDED 에는 안 나옴, 종료일 다음 날부터 ENDED. 목록 항목에 endDate (하루짜리는 null) (#223)")
    void multiDay_statusFilterAndEndDate() {
        saveMultiDay("진행 중 사흘", TODAY.minusDays(1), TODAY.plusDays(1));
        saveMultiDay("어제 끝난 이틀", TODAY.minusDays(3), TODAY.minusDays(1));
        save("어제 하루", TODAY.minusDays(1));
        em.flush();
        LocalDate from = TODAY.minusDays(10);

        CompetitionListResponse closed = competitionQueryService.getList(
                new CompetitionListRequest(from, null, null, null, List.of(CompetitionStatus.CLOSED), null, null));
        CompetitionListResponse ended = competitionQueryService.getList(
                new CompetitionListRequest(from, null, null, null, List.of(CompetitionStatus.ENDED), null, null));
        CompetitionListResponse all = competitionQueryService.getList(request(from, null, null));

        assertThat(names(closed)).containsExactly("진행 중 사흘");
        assertThat(names(ended)).containsExactly("어제 끝난 이틀", "어제 하루");
        assertThat(all.competitions())
                .extracting(CompetitionListItem::name, CompetitionListItem::endDate, CompetitionListItem::status)
                .containsExactly(
                        tuple("어제 끝난 이틀", TODAY.minusDays(1), CompetitionStatus.ENDED),
                        tuple("진행 중 사흘", TODAY.plusDays(1), CompetitionStatus.CLOSED),
                        tuple("어제 하루", null, CompetitionStatus.ENDED));
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
                .audience(CompetitionAudience.ADULT)
                .viewCount(214)
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
        assertThat(detail.name()).isEqualTo("서울 오픈");
        assertThat(detail.region()).isEqualTo(CompetitionRegion.SEOUL);
        assertThat(detail.openDate()).isEqualTo(TODAY.plusDays(20));
        assertThat(detail.endDate()).isNull();   // 하루짜리
        assertThat(detail.registrationStartDate()).isEqualTo(TODAY.minusDays(5));
        assertThat(detail.registrationDeadline()).isEqualTo(TODAY.plusDays(10));
        assertThat(detail.noticeUrl()).isEqualTo("https://example.com/notice");
        assertThat(detail.audience()).isEqualTo(CompetitionAudience.ADULT);
        assertThat(detail.viewCount()).isEqualTo(214);
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
        // 상태 링크 없음 · 접수 사이트 모름 (#223)
        assertThat(detail.links()).isEmpty();
        assertThat(detail.registrationSite()).isNull();
        assertThat(detail.clubRegistrationOnly()).isFalse();
    }

    @Test
    @DisplayName("상세: 여러 날 대회는 endDate, 진행 중(둘째 날)이면 CLOSED (#223)")
    void detail_multiDay() {
        Long id = saveMultiDay("진행 중 사흘", TODAY.minusDays(1), TODAY.plusDays(1)).getId();
        em.flush();

        CompetitionDetailResponse detail = competitionQueryService.getDetail(id);

        assertThat(detail.endDate()).isEqualTo(TODAY.plusDays(1));
        assertThat(detail.status()).isEqualTo(CompetitionStatus.CLOSED);
    }

    // ---- 상태 링크 · 접수 사이트 (#223). 테스트 시계 = TODAY 12:00 (한국 시간) ----

    private static final LocalDateTime NOW = TODAY.atTime(12, 0);
    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

    private Competition saveWithSite(String name, LocalDate openDate, LocalDate endDate, RegistrationSite site,
                                     LocalDate registrationStart, LocalDate registrationDeadline) {
        return em.persist(Competition.builder()
                .name(name).searchName(Competition.normalize(name)).competitionYear(openDate.getYear()).published(true)
                .openDate(openDate).endDate(endDate).registrationSite(site)
                .registrationStartDate(registrationStart).registrationDeadline(registrationDeadline)
                .build());
    }

    private CompetitionLink pendingLink(Competition competition, CompetitionLinkType type) {
        return em.persist(new CompetitionLink(competition, type, "https://example.com/" + competition.getId() + "/" + type.name()));
    }

    private CompetitionLink approvedLink(Competition competition, CompetitionLinkType type, LocalDateTime approvedAt) {
        CompetitionLink link = new CompetitionLink(competition, type, "https://example.com/" + competition.getId() + "/" + type.name());
        link.approve(approvedAt);
        return em.persist(link);
    }

    private CompetitionDetailResponse detailOf(Competition competition) {
        em.flush();
        em.clear();
        return competitionQueryService.getDetail(competition.getId());
    }

    @Test
    @DisplayName("상세 링크: 승인 + 보이는 기간 안만 — 대회 당일 참가자 현황 · 실시간 매트가 겹쳐 나오고, 기간 지난 신청 · 숨긴 대진표 · 아직 이른 결과는 없다. 종류 선언 순서, 기간 값")
    void detail_links_onlyVisibleNow() {
        // 대회 당일(TODAY), 접수는 5일 전에 마감
        Competition competition = saveWithSite("당일 대회", TODAY, null, RegistrationSite.FLOWCOMP, TODAY.minusDays(30), TODAY.minusDays(5));
        // 넣은 순서와 상관없이 종류 순서로 나오는지 보려고 뒤 종류부터 넣는다
        approvedLink(competition, CompetitionLinkType.RESULT, NOW.minusDays(1));                 // 결과는 대회 다음 날부터
        approvedLink(competition, CompetitionLinkType.LIVE_MAT, NOW.minusDays(1));               // 대회일 00:00 ~ 마지막 날 23:59:59
        CompetitionLink bracket = approvedLink(competition, CompetitionLinkType.BRACKET, NOW.minusDays(2));
        bracket.hide();                                                                          // 숨김
        approvedLink(competition, CompetitionLinkType.ENTRY_LIST, NOW.minusDays(3).plusHours(10)); // 승인 시각 ~ 마지막 날
        approvedLink(competition, CompetitionLinkType.REGISTRATION, NOW.minusDays(20));          // 접수 마감 지남

        CompetitionDetailResponse detail = detailOf(competition);

        assertThat(detail.links()).containsExactly(
                new CompetitionLinkResponse(CompetitionLinkType.ENTRY_LIST, "https://example.com/" + competition.getId() + "/ENTRY_LIST",
                        NOW.minusDays(3).plusHours(10), TODAY.atTime(END_OF_DAY)),
                new CompetitionLinkResponse(CompetitionLinkType.LIVE_MAT, "https://example.com/" + competition.getId() + "/LIVE_MAT",
                        TODAY.atStartOfDay(), TODAY.atTime(END_OF_DAY)));
        assertThat(detail.registrationSite()).isEqualTo(RegistrationSite.FLOWCOMP);
        assertThat(detail.clubRegistrationOnly()).isFalse();
    }

    @Test
    @DisplayName("상세 링크: 지금 시각으로 거른다 — 오늘 승인한 참가자 현황 · 대진표는 승인 시각부터 (지금 12:00 → 11:00 승인은 보이고 13:00 승인은 아직 없음)")
    void detail_links_filteredByCurrentTime() {
        Competition competition = saveWithSite("다음 주 대회", TODAY.plusDays(7), null, RegistrationSite.FLOWCOMP, null, null);
        approvedLink(competition, CompetitionLinkType.ENTRY_LIST, TODAY.atTime(11, 0));
        approvedLink(competition, CompetitionLinkType.BRACKET, TODAY.atTime(13, 0));   // 테스트용 미래 승인 시각 — 승인 시각부터 보이는 종류의 시작 경계

        assertThat(detailOf(competition).links())
                .extracting(CompetitionLinkResponse::type, CompetitionLinkResponse::visibleFrom)
                .containsExactly(tuple(CompetitionLinkType.ENTRY_LIST, TODAY.atTime(11, 0)));
    }

    @Test
    @DisplayName("상세 링크: 승인 전 링크는 기간 안이어도 없다. 어제 끝난 대회의 결과는 오늘부터 — 결과 주소 고정 사이트는 끝 없음(null), 그 밖엔 마지막 날 + 14일 23:59:59")
    void detail_links_pendingExcluded_resultUntil() {
        Competition permanent = saveWithSite("플로우컴프 대회", TODAY.minusDays(2), TODAY.minusDays(1), RegistrationSite.FLOWCOMP, null, null);
        Competition street = saveWithSite("스트릿 대회", TODAY.minusDays(1), null, RegistrationSite.STREET_JIUJITSU, null, null);
        Competition unknownSite = saveWithSite("사이트 모름", TODAY.minusDays(1), null, null, null, null);
        Competition pending = saveWithSite("승인 전", TODAY.minusDays(1), null, RegistrationSite.STREET_JIUJITSU, null, null);
        approvedLink(permanent, CompetitionLinkType.RESULT, NOW.minusHours(1));
        approvedLink(street, CompetitionLinkType.RESULT, NOW.minusHours(1));
        approvedLink(unknownSite, CompetitionLinkType.RESULT, NOW.minusHours(1));
        pendingLink(pending, CompetitionLinkType.RESULT);

        assertThat(detailOf(permanent).links()).extracting(CompetitionLinkResponse::type, CompetitionLinkResponse::visibleFrom, CompetitionLinkResponse::visibleUntil)
                .containsExactly(tuple(CompetitionLinkType.RESULT, TODAY.atStartOfDay(), null));
        assertThat(detailOf(street).links()).extracting(CompetitionLinkResponse::visibleUntil)
                .containsExactly(TODAY.minusDays(1).plusDays(14).atTime(END_OF_DAY));
        assertThat(detailOf(unknownSite).links()).extracting(CompetitionLinkResponse::visibleUntil)
                .containsExactly(TODAY.minusDays(1).plusDays(14).atTime(END_OF_DAY));
        assertThat(detailOf(pending).links()).isEmpty();
    }

    @Test
    @DisplayName("상세: 대한체육회 대회는 도장 신청(clubRegistrationOnly) · 접수 사이트 KSOC, 승인된 신청 링크가 남아 있어도(동시 요청) 내보내지 않고 다른 링크는 그대로")
    void detail_clubRegistrationOnly() {
        // 접수 중 (접수 시작 5일 전, 마감 3일 뒤) — 신청 링크가 대한체육회가 아니었다면 보였을 기간
        Competition competition = saveWithSite("전국 선수권", TODAY.plusDays(10), TODAY.plusDays(11), RegistrationSite.KSOC,
                TODAY.minusDays(5), TODAY.plusDays(3));
        approvedLink(competition, CompetitionLinkType.REGISTRATION, NOW.minusDays(1));
        approvedLink(competition, CompetitionLinkType.ENTRY_LIST, NOW.minusDays(1));

        CompetitionDetailResponse detail = detailOf(competition);

        assertThat(detail.registrationSite()).isEqualTo(RegistrationSite.KSOC);
        assertThat(detail.clubRegistrationOnly()).isTrue();
        assertThat(detail.links()).extracting(CompetitionLinkResponse::type).containsExactly(CompetitionLinkType.ENTRY_LIST);
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
    @DisplayName("상세: 이 대회 알림은 로그인 + 관심 + 알림 켜짐일 때만 true (비로그인·관심 없음·끔은 false)")
    void detail_noticeEnabled() {
        Competition competition = saveDetailed();
        em.flush();
        assertThat(competitionQueryService.getDetail(competition.getId()).noticeEnabled()).isFalse();

        login();
        assertThat(competitionQueryService.getDetail(competition.getId()).noticeEnabled()).isFalse();

        CompetitionInterest interest = em.persist(new CompetitionInterest(em.find(Competition.class, competition.getId()), me));
        em.flush();
        assertThat(competitionQueryService.getDetail(competition.getId()).noticeEnabled()).isTrue();

        interest.disableNotice();
        em.flush();
        CompetitionDetailResponse muted = competitionQueryService.getDetail(competition.getId());
        assertThat(muted.noticeEnabled()).isFalse();
        assertThat(muted.interested()).isTrue();
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
        assertThat(competitionQueryService.getDetail(id).viewCount()).isEqualTo(214);
        // 목록 항목에도 같은 조회수
        assertThat(competitionQueryService.getList(request(null, null, 10)).competitions())
                .filteredOn(item -> item.competitionId().equals(id))
                .singleElement().extracting(CompetitionListItem::viewCount).isEqualTo(214L);
    }

    private static List<String> names(CompetitionPickerResponse response) {
        return response.competitions().stream().map(CompetitionListItem::name).toList();
    }

    @Test
    @DisplayName("대회 고르기: 공개 대회만, 끝나지 않은 대회(오늘 포함, 대회일 순) 다음에 끝난 대회(대회일 최근 순)")
    void picker_order() {
        seedTimeline();

        CompetitionPickerResponse response = competitionQueryService.getPicker(null);

        assertThat(response.today()).isEqualTo(TODAY);
        assertThat(names(response)).containsExactly("오늘1", "오늘2", "미래1", "미래2", "미래3", "과거2", "과거1");
    }

    @Test
    @DisplayName("대회 고르기: 진행 중인 여러 날 대회는 끝나지 않은 쪽(대회일 순 맨 앞), 끝난 여러 날 대회는 끝난 쪽 (#223)")
    void picker_multiDayByLastDay() {
        seedTimeline();
        saveMultiDay("진행 중 사흘", TODAY.minusDays(1), TODAY.plusDays(1));
        saveMultiDay("끝난 이틀", TODAY.minusDays(3), TODAY.minusDays(2));
        em.flush();

        assertThat(names(competitionQueryService.getPicker(null)))
                .containsExactly("진행 중 사흘", "오늘1", "오늘2", "미래1", "미래2", "미래3", "과거2", "끝난 이틀", "과거1");
    }

    @Test
    @DisplayName("대회 고르기: 이름 일부(공백·대소문자 무시)로 찾고, 빈 검색어는 전체")
    void picker_keyword() {
        save("서울 오픈", TODAY.plusDays(3));
        save("서울오픈 윈터", TODAY.minusDays(3));
        save("부산 오픈", TODAY.plusDays(1));
        save("Seoul Open", TODAY.plusDays(2));
        em.flush();

        assertThat(names(competitionQueryService.getPicker(" 서울오 "))).containsExactly("서울 오픈", "서울오픈 윈터");
        assertThat(names(competitionQueryService.getPicker("seoulopen"))).containsExactly("Seoul Open");
        assertThat(names(competitionQueryService.getPicker("  "))).hasSize(4);
        assertThat(names(competitionQueryService.getPicker("없는대회"))).isEmpty();
    }

    @Test
    @DisplayName("대회 고르기: 최대 20개 — 끝나지 않은 대회가 20개를 채우면 끝난 대회는 안 나오고, 모자라면 끝난 대회로 채운다")
    void picker_limit() {
        for (int i = 1; i <= 18; i++) {
            save("미래" + i, TODAY.plusDays(i));
        }
        for (int i = 1; i <= 4; i++) {
            save("과거" + i, TODAY.minusDays(i));
        }
        em.flush();

        assertThat(names(competitionQueryService.getPicker(null)))
                .hasSize(20)
                .endsWith("미래18", "과거1", "과거2");

        for (int i = 19; i <= 22; i++) {
            save("미래" + i, TODAY.plusDays(i));
        }
        em.flush();
        assertThat(names(competitionQueryService.getPicker(null)))
                .hasSize(20)
                .noneMatch(name -> name.startsWith("과거"));
    }

    @Test
    @DisplayName("게시글 대회 카드: 공개 대회만 카드로, 관심 수·내 관심 여부 포함")
    void cards() {
        Competition published = save("서울 오픈", TODAY.plusDays(3));
        Long catalog = em.persist(Competition.of("카탈로그", 2026)).getId();
        em.persist(new CompetitionInterest(em.find(Competition.class, published.getId()), me));
        em.flush();
        login();

        Map<Long, CompetitionListItem> cards = competitionQueryService.getCards(List.of(published.getId(), catalog, -1L));

        assertThat(cards).containsOnlyKeys(published.getId());
        CompetitionListItem card = cards.get(published.getId());
        assertThat(card.name()).isEqualTo("서울 오픈");
        assertThat(card.interestCount()).isEqualTo(1);
        assertThat(card.interested()).isTrue();
        assertThat(competitionQueryService.getCards(List.of())).isEmpty();
    }

    @Test
    @DisplayName("게시글 연결용 공개 대회: 없거나 미공개면 COMPETITION_NOT_FOUND")
    void getPublished() {
        Long id = save("서울 오픈", TODAY).getId();
        Long catalog = em.persist(Competition.of("카탈로그", 2026)).getId();
        em.flush();

        assertThat(competitionQueryService.getPublished(id).getId()).isEqualTo(id);
        assertThatThrownBy(() -> competitionQueryService.getPublished(catalog))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
    }

    @Test
    @DisplayName("지역: 지역 enum 선언 순서로, 지난 대회 지역도 포함")
    void regionsInEnumOrder() {
        saveIn("제주 오픈", CompetitionRegion.JEJU, TODAY.plusDays(3));
        saveIn("경기 오픈", CompetitionRegion.GYEONGGI, TODAY.minusDays(10));
        saveIn("서울 오픈", CompetitionRegion.SEOUL, TODAY.plusDays(5));
        em.flush();

        CompetitionRegionsResponse response = competitionQueryService.getRegions();

        assertThat(response.regions()).containsExactly(CompetitionRegion.SEOUL, CompetitionRegion.GYEONGGI, CompetitionRegion.JEJU);
    }

    @Test
    @DisplayName("지역: 공개 대회가 없으면 빈 배열")
    void regionsEmpty() {
        em.persist(Competition.of("카탈로그", 2026));
        em.flush();

        assertThat(competitionQueryService.getRegions().regions()).isEmpty();
    }

    // ---- 관심 많은 대회 (#210) — 테스트 설정: 상위 2개, 관심 2개 이상 ----

    private void interests(Competition competition, int count) {
        for (int i = 0; i < count; i++) {
            em.persist(new CompetitionInterest(competition, em.persist(User.builder()
                    .snsProvider(SnsProvider.KAKAO)
                    .snsId(UUID.randomUUID().toString())
                    .role(UserRole.USER)
                    .status(UserStatus.ACTIVE)
                    .build())));
        }
    }

    @Test
    @DisplayName("관심 많은 대회: 지역 필터를 걸어도 같은 달 전체 기준 순위, 끝난 대회는 false, 게시글 대회 카드도 같은 값")
    void popular_monthWideIgnoringFilters() {
        Competition seoulTop = saveIn("서울 1등", CompetitionRegion.SEOUL, TODAY.plusDays(3));
        Competition busanSecond = saveIn("부산 2등", CompetitionRegion.BUSAN, TODAY.plusDays(4));
        Competition seoulThird = saveIn("서울 3등", CompetitionRegion.SEOUL, TODAY.plusDays(5));
        Competition endedHot = saveIn("끝난 인기", CompetitionRegion.SEOUL, TODAY.minusDays(2));
        interests(seoulTop, 4);
        interests(busanSecond, 3);
        interests(seoulThird, 2);
        interests(endedHot, 9);

        CompetitionListResponse seoulOnly = competitionQueryService.getList(
                new CompetitionListRequest(TODAY.withDayOfMonth(1), null, null, List.of(CompetitionRegion.SEOUL), null, null, null));

        assertThat(seoulOnly.competitions())
                .extracting(CompetitionListItem::name, CompetitionListItem::popular)
                .containsExactly(
                        tuple("끝난 인기", false),
                        tuple("서울 1등", true),
                        tuple("서울 3등", false));   // 같은 달 3등 (부산 2등이 위)
        assertThat(competitionQueryService.getCards(List.of(busanSecond.getId())).get(busanSecond.getId()).popular()).isTrue();
    }

    @Test
    @DisplayName("관심 많은 대회: 진행 중인 여러 날 대회는 끝나지 않은 대회로 순위에 들고, 종료일이 지난 여러 날 대회는 false (#223)")
    void popular_multiDayByLastDay() {
        Competition inProgress = saveMultiDay("진행 중 사흘", TODAY.minusDays(1), TODAY.plusDays(1));
        Competition endedMultiDay = saveMultiDay("끝난 이틀", TODAY.minusDays(3), TODAY.minusDays(1));
        Competition upcoming = save("다가오는 대회", TODAY.plusDays(3));
        Competition upcomingLow = save("다가오는 3등", TODAY.plusDays(4));
        interests(inProgress, 4);
        interests(endedMultiDay, 9);
        interests(upcoming, 3);
        interests(upcomingLow, 2);

        CompetitionListResponse list = competitionQueryService.getList(request(TODAY.withDayOfMonth(1), null, null));

        assertThat(list.competitions())
                .extracting(CompetitionListItem::name, CompetitionListItem::popular)
                .containsExactly(
                        tuple("끝난 이틀", false),
                        tuple("진행 중 사흘", true),
                        tuple("다가오는 대회", true),
                        tuple("다가오는 3등", false));   // 끝난 이틀(관심 9)은 순위에서 빠지고 진행 중 · 다가오는 대회가 상위 2개
    }

    @Test
    @DisplayName("관심 많은 대회: 지난달에 시작해 오늘 끝나는 여러 날 대회도 그 달(대회일 기준) 순위에 든다 — 그 달에 끝나지 않은 대회가 그것뿐이어도 (#223)")
    void popular_multiDayStartedLastMonth() {
        // 10/31 ~ 11/9(오늘) — 대회일은 10월, 아직 끝나지 않음. 10월의 다른 대회는 끝났음
        Competition spansMonths = saveMultiDay("10월 말 시작 여러 날 대회", LocalDate.of(2026, 10, 31), TODAY);
        Competition octoberEnded = save("10월 끝난 대회", LocalDate.of(2026, 10, 20));
        interests(spansMonths, 2);
        interests(octoberEnded, 9);

        CompetitionListResponse list = competitionQueryService.getList(request(LocalDate.of(2026, 10, 1), null, null));

        assertThat(list.competitions())
                .extracting(CompetitionListItem::name, CompetitionListItem::popular)
                .containsExactly(
                        tuple("10월 끝난 대회", false),
                        tuple("10월 말 시작 여러 날 대회", true));
    }

    @Test
    @DisplayName("관심 많은 대회: 한 응답이 두 달에 걸치면 달마다 따로 상위 N개, 글쓰기 대회 고르기도 같은 값")
    void popular_perMonth_listAndPicker() {
        Competition novTop = save("11월 1등", TODAY.plusDays(5));       // 11/14
        Competition novLow = save("11월 3등", TODAY.plusDays(6));
        Competition novSecond = save("11월 2등", TODAY.plusDays(7));
        Competition decTop = save("12월 1등", LocalDate.of(2026, 12, 6));
        interests(novTop, 5);
        interests(novSecond, 4);
        interests(novLow, 3);
        interests(decTop, 2);

        CompetitionListResponse list = competitionQueryService.getList(request(TODAY, null, 10));
        assertThat(list.competitions())
                .extracting(CompetitionListItem::name, CompetitionListItem::popular)
                .containsExactly(
                        tuple("11월 1등", true),
                        tuple("11월 3등", false),   // 11월은 관심 5·4 두 대회가 상위 2개
                        tuple("11월 2등", true),
                        tuple("12월 1등", true));   // 12월은 따로 순위 (관심 2개 = M)
        assertThat(competitionQueryService.getPicker(null).competitions())
                .extracting(CompetitionListItem::name, CompetitionListItem::popular)
                .contains(tuple("11월 2등", true), tuple("11월 3등", false), tuple("12월 1등", true));
    }
}

