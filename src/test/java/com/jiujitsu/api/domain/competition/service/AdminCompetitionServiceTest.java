package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionDetailResponse;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListItem;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionListRequest;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionRequest;
import com.jiujitsu.api.domain.competition.dto.AdminCompetitionSaveResponse;
import com.jiujitsu.api.domain.competition.dto.CompetitionFeeResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.entity.CompetitionFee;
import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import com.jiujitsu.api.domain.competition.entity.CompetitionRegion;
import com.jiujitsu.api.domain.competition.entity.CompetitionRequiredField;
import com.jiujitsu.api.domain.competition.entity.CompetitionUniform;
import com.jiujitsu.api.domain.competition.mapper.CompetitionMapper;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionRecord;
import com.jiujitsu.api.domain.community.profile.event.CompetitionRenamedEventListener;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// 필터 조합·정렬·페이지는 실제 쿼리 결과에 달려 있어 DB 와 함께 검증한다 (CompetitionQueryServiceTest 와 같은 방식)
@DataJpaTest
@Import({AdminCompetitionService.class, CompetitionInterestService.class, CompetitionMapper.class, CompetitionRenamedEventListener.class})
class AdminCompetitionServiceTest {

    private static final LocalDate BASE = LocalDate.of(2026, 11, 13);
    private static final PageRequest FIRST_PAGE = PageRequest.of(0, 20);
    private static final AdminCompetitionListRequest ALL = new AdminCompetitionListRequest(null, null, null, null);

    @Autowired
    private AdminCompetitionService adminCompetitionService;

    @Autowired
    private TestEntityManager em;

    @MockBean   // CompetitionInterestService 의존성. 관리자 조회는 쓰지 않는다
    private AuthenticationFacade authenticationFacade;

    private Competition save(String name, LocalDate openDate, boolean published) {
        return em.persist(Competition.builder()
                .name(name)
                .searchName(Competition.normalize(name))
                .competitionYear(openDate.getYear())
                .published(published)
                .openDate(openDate)
                .build());
    }

    // 자동완성용 카탈로그 엔트리 (이름·연도만)
    private Competition saveCatalog(String name, int year) {
        return em.persist(Competition.of(name, year));
    }

    private User saveUser() {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private static LocalDate day(int offset) {
        return BASE.plusDays(offset);
    }

    private static List<String> names(Page<AdminCompetitionListItem> page) {
        return page.getContent().stream().map(AdminCompetitionListItem::name).toList();
    }

    @Test
    @DisplayName("목록: 공개·미공개 모두 대회일·ID 오름차순, 자동완성용 대회(대회일 없음)는 빠진다")
    void getList_publishedAndUnpublished_excludesCatalog() {
        save("다가오는 공개", day(10), true);
        save("지난 공개", day(-30), true);
        save("준비 중", day(20), false);
        save("같은 날 두번째", day(10), true);
        saveCatalog("작년 대회", 2025);

        Page<AdminCompetitionListItem> page = adminCompetitionService.getList(ALL, FIRST_PAGE);

        assertThat(names(page)).containsExactly("지난 공개", "다가오는 공개", "같은 날 두번째", "준비 중");
        assertThat(page.getTotalElements()).isEqualTo(4);
    }

    @Test
    @DisplayName("목록: published 로 공개·미공개를 거른다")
    void getList_publishedFilter() {
        save("공개", day(1), true);
        save("미공개", day(2), false);
        saveCatalog("자동완성", 2025);

        assertThat(names(adminCompetitionService.getList(new AdminCompetitionListRequest(true, null, null, null), FIRST_PAGE)))
                .containsExactly("공개");
        assertThat(names(adminCompetitionService.getList(new AdminCompetitionListRequest(false, null, null, null), FIRST_PAGE)))
                .containsExactly("미공개");
    }

    @Test
    @DisplayName("목록: 대회명 검색은 공백·대소문자를 무시하고 미공개 대회도 찾는다")
    void getList_keyword() {
        save("Seoul Open 2026", day(1), false);
        save("부산 오픈", day(2), true);

        assertThat(names(adminCompetitionService.getList(new AdminCompetitionListRequest(null, " seoulOPEN ", null, null), FIRST_PAGE)))
                .containsExactly("Seoul Open 2026");
    }

    @Test
    @DisplayName("목록: 검색어의 % _ 는 글자 그대로 찾는다")
    void getList_keywordEscapesLike() {
        save("100% 오픈", day(1), true);
        save("100 오픈", day(2), true);

        assertThat(names(adminCompetitionService.getList(new AdminCompetitionListRequest(null, "100%", null, null), FIRST_PAGE)))
                .containsExactly("100% 오픈");
    }

    @Test
    @DisplayName("목록: 대회일 기간은 from·to 당일을 포함하고, 하나만 줘도 된다")
    void getList_dateRange() {
        save("전날", day(-1), true);
        save("시작일", day(0), true);
        save("끝일", day(5), true);
        save("다음날", day(6), true);

        assertThat(names(adminCompetitionService.getList(new AdminCompetitionListRequest(null, null, day(0), day(5)), FIRST_PAGE)))
                .containsExactly("시작일", "끝일");
        assertThat(names(adminCompetitionService.getList(new AdminCompetitionListRequest(null, null, day(5), null), FIRST_PAGE)))
                .containsExactly("끝일", "다음날");
        assertThat(names(adminCompetitionService.getList(new AdminCompetitionListRequest(null, null, null, day(0)), FIRST_PAGE)))
                .containsExactly("전날", "시작일");
    }

    @Test
    @DisplayName("목록: from 이 to 보다 뒤면 WRONG_PARAMETER")
    void getList_fromAfterTo() {
        assertThatThrownBy(() -> adminCompetitionService.getList(new AdminCompetitionListRequest(null, null, day(1), day(0)), FIRST_PAGE))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
    }

    @Test
    @DisplayName("목록: 클라이언트 sort 는 무시하고 대회일 순, 페이지 번호·크기는 따른다")
    void getList_ignoresSortKeepsPaging() {
        save("가", day(1), true);
        save("나", day(2), true);
        save("다", day(3), true);

        Page<AdminCompetitionListItem> second = adminCompetitionService.getList(ALL, PageRequest.of(1, 2, Sort.by(Sort.Order.desc("name"))));

        assertThat(names(second)).containsExactly("다");
        assertThat(second.getTotalElements()).isEqualTo(3);
        assertThat(second.getTotalPages()).isEqualTo(2);
    }

    @Test
    @DisplayName("목록: 한 줄 칸과 비어 있는 필수 칸")
    void getList_itemFields() {
        Competition saved = em.persist(Competition.builder()
                .name("서울 오픈")
                .searchName(Competition.normalize("서울 오픈"))
                .competitionYear(2026)
                .published(true)
                .openDate(day(10))
                .region(CompetitionRegion.SEOUL)
                .place("서울 송파")
                .uniforms(new LinkedHashSet<>(Set.of(CompetitionUniform.GI)))
                .registrationDeadline(day(5))
                .noticeUrl("https://example.com")
                .lastVerifiedDate(day(-11))
                .build());

        AdminCompetitionListItem item = adminCompetitionService.getList(ALL, FIRST_PAGE).getContent().get(0);

        assertThat(item).isEqualTo(new AdminCompetitionListItem(saved.getId(), "서울 오픈", 2026, true, day(10),
                CompetitionRegion.SEOUL, "서울 송파", day(5), day(-11),
                List.of(CompetitionRequiredField.START_TIME, CompetitionRequiredField.REGISTRATION_START_DATE)));
    }

    @Test
    @DisplayName("상세: 공개 대회의 전체 칸, 조회수·관심 수")
    void getDetail_published() {
        Competition saved = em.persist(Competition.builder()
                .name("서울 오픈")
                .searchName(Competition.normalize("서울 오픈"))
                .competitionYear(2026)
                .published(true)
                .openDate(day(20))
                .startTime(LocalTime.of(9, 0))
                .region(CompetitionRegion.SEOUL)
                .place("서울 송파")
                .venue("올림픽홀")
                .host("서울시주짓수회")
                .noticeUrl("https://example.com")
                .uniforms(new LinkedHashSet<>(List.of(CompetitionUniform.NO_GI, CompetitionUniform.GI)))
                .registrationStartDate(day(-10))
                .earlyBirdDeadline(day(2))
                .registrationDeadline(day(10))
                .fees(new ArrayList<>(List.of(new CompetitionFee("얼리버드", 60000, day(2)), new CompetitionFee("기본 참가비", 70000, null))))
                .feeNote("성인 1종목 기준")
                .lastVerifiedDate(day(0))
                .viewCount(7)
                .build());
        em.persist(new CompetitionInterest(saved, saveUser()));
        em.persist(new CompetitionInterest(saved, saveUser()));

        AdminCompetitionDetailResponse detail = adminCompetitionService.getDetail(saved.getId());

        assertThat(detail.competitionId()).isEqualTo(saved.getId());
        assertThat(detail.published()).isTrue();
        assertThat(detail.startTime()).isEqualTo(LocalTime.of(9, 0));
        assertThat(detail.venue()).isEqualTo("올림픽홀");
        assertThat(detail.host()).isEqualTo("서울시주짓수회");
        assertThat(detail.uniforms()).containsExactly(CompetitionUniform.GI, CompetitionUniform.NO_GI);
        assertThat(detail.registrationStartDate()).isEqualTo(day(-10));
        assertThat(detail.earlyBirdDeadline()).isEqualTo(day(2));
        assertThat(detail.fees()).containsExactly(
                new CompetitionFeeResponse("얼리버드", 60000, day(2)),
                new CompetitionFeeResponse("기본 참가비", 70000, null));
        assertThat(detail.feeNote()).isEqualTo("성인 1종목 기준");
        assertThat(detail.viewCount()).isEqualTo(7);
        assertThat(detail.interestCount()).isEqualTo(2);
        assertThat(detail.missingFields()).isEmpty();
    }

    @Test
    @DisplayName("상세: 미공개 대회도 열리고 (접수 상태를 부르지 않음), 조회수는 오르지 않는다")
    void getDetail_unpublished() {
        Competition saved = save("준비 중", day(20), false);

        AdminCompetitionDetailResponse detail = adminCompetitionService.getDetail(saved.getId());
        em.clear();

        assertThat(detail.published()).isFalse();
        assertThat(detail.interestCount()).isZero();
        assertThat(em.find(Competition.class, saved.getId()).getViewCount()).isZero();
    }

    @Test
    @DisplayName("상세: 자동완성용 대회(대회일 없음)도 ID 로는 열리고, 모든 필수 칸이 비어 있다")
    void getDetail_catalogEntry() {
        Competition saved = saveCatalog("작년 대회", 2025);

        AdminCompetitionDetailResponse detail = adminCompetitionService.getDetail(saved.getId());

        assertThat(detail.openDate()).isNull();
        assertThat(detail.competitionYear()).isEqualTo(2025);
        assertThat(detail.missingFields()).containsExactly(CompetitionRequiredField.values());
    }

    @Test
    @DisplayName("상세: 없는 ID 는 COMPETITION_NOT_FOUND")
    void getDetail_notFound() {
        assertThatThrownBy(() -> adminCompetitionService.getDetail(999_999L))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
    }

    // ---- 만들기·고치기 (#208) ----

    private static AdminCompetitionRequest request(String name, LocalDate openDate) {
        return new AdminCompetitionRequest(name, openDate.getYear(), openDate, LocalTime.of(9, 0), CompetitionRegion.SEOUL,
                " 서울 송파 ", "올림픽홀", "서울시주짓수회", "https://example.com/notice", List.of(CompetitionUniform.GI, CompetitionUniform.GI),
                null, openDate.minusDays(30), openDate.minusDays(20), openDate.minusDays(10),
                List.of(new AdminCompetitionRequest.Fee("얼리버드", 60000, openDate.minusDays(20)), new AdminCompetitionRequest.Fee("기본 참가비", 70000, null)),
                "성인 1종목 기준");
    }

    // 이름·연도·대회일만, 나머지 칸 비움
    private static AdminCompetitionRequest minimal(String name, int year, LocalDate openDate) {
        return new AdminCompetitionRequest(name, year, openDate, null, null, "  ", null, null, null, null, null,
                null, null, null, null, null);
    }

    private CompetitionRecord saveRecord(Competition competition, String name, int year) {
        CommunityProfile profile = em.persist(CommunityProfile.builder().user(saveUser()).build());
        return em.persist(CompetitionRecord.builder()
                .communityProfile(profile)
                .competition(competition)
                .competitionName(name)
                .competitionYear(year)
                .build());
    }

    private CompetitionRecord reload(CompetitionRecord record) {
        em.flush();
        em.clear();
        return em.find(CompetitionRecord.class, record.getId());
    }

    @Test
    @DisplayName("만들기: 새 대회는 미공개로 모든 칸을 저장하고, 글자 칸 공백 정리·종목 중복 제거")
    void create_new() {
        AdminCompetitionSaveResponse response = adminCompetitionService.create(request("서울 오픈", day(30)));

        assertThat(response.catalogFilled()).isFalse();
        AdminCompetitionDetailResponse detail = response.competition();
        assertThat(detail.published()).isFalse();
        assertThat(detail.name()).isEqualTo("서울 오픈");
        assertThat(detail.place()).isEqualTo("서울 송파");
        assertThat(detail.uniforms()).containsExactly(CompetitionUniform.GI);
        assertThat(detail.fees()).hasSize(2);
        assertThat(detail.earlyBirdDeadline()).isEqualTo(day(10));
        assertThat(detail.lastVerifiedDate()).isNull();
        em.clear();
        assertThat(em.find(Competition.class, detail.competitionId()).getSearchName()).isEqualTo("서울오픈");
    }

    @Test
    @DisplayName("만들기: 공백만 있는 글자 칸은 null 로 저장해 missingFields 에 빈 칸으로 잡힌다")
    void create_blankTextIsNull() {
        AdminCompetitionDetailResponse detail = adminCompetitionService.create(minimal("서울 오픈", 2026, day(30))).competition();

        assertThat(detail.place()).isNull();
        assertThat(detail.missingFields()).contains(CompetitionRequiredField.NOTICE_URL, CompetitionRequiredField.REGION);
    }

    @Test
    @DisplayName("만들기: 같은 이름·연도의 자동완성용 대회가 있으면 그 행에 채우고, 연결된 프로필 기록 이름도 맞춘다")
    void create_fillsCatalogEntry() {
        Competition catalog = saveCatalog("서울오픈", 2026);
        CompetitionRecord record = saveRecord(catalog, "서울오픈", 2026);

        AdminCompetitionSaveResponse response = adminCompetitionService.create(request("서울 오픈", LocalDate.of(2026, 12, 6)));

        assertThat(response.catalogFilled()).isTrue();
        assertThat(response.competition().competitionId()).isEqualTo(catalog.getId());
        assertThat(response.competition().name()).isEqualTo("서울 오픈");
        assertThat(response.competition().published()).isFalse();
        assertThat(response.competition().openDate()).isEqualTo(LocalDate.of(2026, 12, 6));
        assertThat(reload(record).getCompetitionName()).isEqualTo("서울 오픈");
    }

    @Test
    @DisplayName("만들기: 대회일 있는 대회와 이름(공백·대소문자 무시)·연도가 같으면 COMPETITION_DUPLICATED")
    void create_duplicateOfEventCompetition() {
        save("Seoul Open", LocalDate.of(2026, 12, 6), true);

        assertThatThrownBy(() -> adminCompetitionService.create(request("seoul  OPEN", LocalDate.of(2026, 10, 1))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_DUPLICATED);
    }

    @Test
    @DisplayName("만들기: 연도가 다르면 같은 이름이라도 따로 만든다")
    void create_sameNameOtherYear() {
        save("서울 오픈", LocalDate.of(2025, 12, 6), true);

        assertThat(adminCompetitionService.create(request("서울 오픈", LocalDate.of(2026, 12, 6))).catalogFilled()).isFalse();
    }

    @Test
    @DisplayName("만들기·고치기: 연도가 대회일 연도와 다르면 COMPETITION_INVALID_EVENT_INFO")
    void yearMustMatchOpenDate() {
        Competition existing = save("부산 오픈", day(30), false);

        assertThatThrownBy(() -> adminCompetitionService.create(minimal("서울 오픈", 2025, LocalDate.of(2026, 12, 6))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
        assertThatThrownBy(() -> adminCompetitionService.update(existing.getId(), minimal("부산 오픈", 2027, LocalDate.of(2026, 12, 6))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
    }

    @Test
    @DisplayName("만들기: 정리하면 비는 이름은 WRONG_PARAMETER")
    void create_invalidName() {
        assertThatThrownBy(() -> adminCompetitionService.create(minimal("\u200B\u3000", 2026, day(30))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
    }

    @Test
    @DisplayName("만들기: 일정 순서가 틀리면 COMPETITION_INVALID_EVENT_INFO 이고 저장되지 않는다")
    void create_invalidSchedule() {
        AdminCompetitionRequest reversed = new AdminCompetitionRequest("서울 오픈", 2026, day(30), null, null, null, null, null, null, null, null,
                null, null, day(40), null, null);

        assertThatThrownBy(() -> adminCompetitionService.create(reversed))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
        assertThat(adminCompetitionService.getList(ALL, FIRST_PAGE).getTotalElements()).isZero();
    }

    @Test
    @DisplayName("고치기: 전체 교체 — 안 보낸 칸은 비우고, 공개 여부·마지막 확인일·조회수는 그대로")
    void update_replacesAllKeepsPublishState() {
        Competition saved = em.persist(Competition.builder()
                .name("서울 오픈").searchName("서울오픈").competitionYear(2026)
                .published(true).openDate(LocalDate.of(2026, 12, 6)).startTime(LocalTime.of(9, 0))
                .region(CompetitionRegion.SEOUL).venue("올림픽홀")
                .uniforms(new LinkedHashSet<>(Set.of(CompetitionUniform.GI)))
                .fees(new ArrayList<>(List.of(new CompetitionFee("기본", 50000, null))))
                .lastVerifiedDate(LocalDate.of(2026, 11, 2)).viewCount(9)
                .build());

        AdminCompetitionDetailResponse detail = adminCompetitionService.update(saved.getId(), minimal("서울 오픈", 2026, LocalDate.of(2026, 12, 13)));

        assertThat(detail.openDate()).isEqualTo(LocalDate.of(2026, 12, 13));
        assertThat(detail.startTime()).isNull();
        assertThat(detail.venue()).isNull();
        assertThat(detail.uniforms()).isEmpty();
        assertThat(detail.fees()).isEmpty();
        assertThat(detail.published()).isTrue();
        assertThat(detail.lastVerifiedDate()).isEqualTo(LocalDate.of(2026, 11, 2));
        assertThat(detail.viewCount()).isEqualTo(9);
        em.flush();
        em.clear();
        Competition reloaded = em.find(Competition.class, saved.getId());
        assertThat(reloaded.getFees()).isEmpty();
        assertThat(reloaded.getVenue()).isNull();
    }

    @Test
    @DisplayName("고치기: 참가비 단계와 얼리버드 마감을 한 요청에서 같이 바꾼다")
    void update_feesWithEarlyBird() {
        Competition saved = save("서울 오픈", LocalDate.of(2026, 12, 6), true);

        AdminCompetitionDetailResponse detail = adminCompetitionService.update(saved.getId(), request("서울 오픈", LocalDate.of(2026, 12, 6)));

        assertThat(detail.earlyBirdDeadline()).isEqualTo(LocalDate.of(2026, 11, 16));
        assertThat(detail.fees()).containsExactly(
                new CompetitionFeeResponse("얼리버드", 60000, LocalDate.of(2026, 11, 16)),
                new CompetitionFeeResponse("기본 참가비", 70000, null));
    }

    @Test
    @DisplayName("고치기: 자동완성용 대회는 COMPETITION_CATALOG_NOT_EDITABLE, 없는 ID 는 COMPETITION_NOT_FOUND")
    void update_catalogAndNotFound() {
        Competition catalog = saveCatalog("작년 대회", 2026);

        assertThatThrownBy(() -> adminCompetitionService.update(catalog.getId(), minimal("작년 대회", 2026, day(30))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_CATALOG_NOT_EDITABLE);
        assertThatThrownBy(() -> adminCompetitionService.update(999_999L, minimal("없음", 2026, day(30))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_NOT_FOUND);
    }

    @Test
    @DisplayName("고치기: 다른 대회(자동완성용 포함)와 이름·연도가 겹치면 COMPETITION_DUPLICATED, 자기 이름 표기만 바꾸면 통과")
    void update_renameDuplicate() {
        Competition target = save("서울 오픈", LocalDate.of(2026, 12, 6), false);
        save("부산 오픈", LocalDate.of(2026, 12, 13), true);
        saveCatalog("대구 오픈", 2026);

        assertThatThrownBy(() -> adminCompetitionService.update(target.getId(), minimal("부산오픈", 2026, LocalDate.of(2026, 12, 6))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_DUPLICATED);
        assertThatThrownBy(() -> adminCompetitionService.update(target.getId(), minimal("대구 오픈", 2026, LocalDate.of(2026, 12, 6))))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_DUPLICATED);
        assertThat(adminCompetitionService.update(target.getId(), minimal("서울오픈", 2026, LocalDate.of(2026, 12, 6))).name())
                .isEqualTo("서울오픈");
    }

    @Test
    @DisplayName("고치기: 이름·연도를 바꾸면 그 대회를 고른 프로필 기록만 맞추고, 다른 기록은 그대로")
    void update_renameSyncsRecords() {
        Competition target = save("서울 오픈", LocalDate.of(2026, 3, 6), true);
        Competition other = save("부산 오픈", LocalDate.of(2026, 4, 6), true);
        CompetitionRecord first = saveRecord(target, "서울 오픈", 2026);
        CompetitionRecord second = saveRecord(target, "서울 오픈", 2026);
        CompetitionRecord otherRecord = saveRecord(other, "부산 오픈", 2026);
        CompetitionRecord typed = saveRecord(null, "서울 오픈", 2026);   // 직접 입력 (연결 없음)

        adminCompetitionService.update(target.getId(), minimal("제1회 서울 오픈", 2026, LocalDate.of(2026, 3, 6)));

        assertThat(reload(first).getCompetitionName()).isEqualTo("제1회 서울 오픈");
        assertThat(reload(second).getCompetitionName()).isEqualTo("제1회 서울 오픈");
        assertThat(reload(otherRecord).getCompetitionName()).isEqualTo("부산 오픈");
        assertThat(reload(typed).getCompetitionName()).isEqualTo("서울 오픈");
    }

    @Test
    @DisplayName("고치기: 이름·연도가 그대로면 프로필 기록을 건드리지 않는다")
    void update_noRenameKeepsRecords() {
        Competition target = save("서울 오픈", LocalDate.of(2026, 3, 6), true);
        CompetitionRecord record = saveRecord(target, "서울오픈 (예전 표기)", 2026);

        adminCompetitionService.update(target.getId(), minimal("서울 오픈", 2026, LocalDate.of(2026, 3, 7)));

        assertThat(reload(record).getCompetitionName()).isEqualTo("서울오픈 (예전 표기)");
    }
}

