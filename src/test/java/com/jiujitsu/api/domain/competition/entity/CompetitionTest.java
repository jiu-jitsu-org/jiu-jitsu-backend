package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompetitionTest {

    @Test
    @DisplayName("NBSP·전각 공백도 공백으로 보고 정규화한다 (웹 페이지에서 복사한 대회명)")
    void normalize_unicodeSpaces() {
        assertThat(Competition.normalize("서울 오픈")).isEqualTo("서울오픈");
        assertThat(Competition.normalize("서울　오픈")).isEqualTo("서울오픈");
    }

    @Test
    @DisplayName("표시용 이름은 앞뒤 공백을 지우고 연속 공백(NBSP 포함)을 하나로 줄인다")
    void of_cleansDisplayName() {
        Competition competition = Competition.of(" 서울   오픈　", 2026);

        assertThat(competition.getName()).isEqualTo("서울 오픈");
        assertThat(competition.getSearchName()).isEqualTo("서울오픈");
    }

    @Test
    @DisplayName("폭 없는 공백·BOM 은 지우고, NFD 한글·전각 영숫자는 NFKC 로 통일한다")
    void normalize_invisiblesAndUnicodeForms() {
        assertThat(Competition.normalize("서울\u200B오픈\uFEFF")).isEqualTo("서울오픈");
        assertThat(Competition.normalize(Normalizer.normalize("서울", Normalizer.Form.NFD))).isEqualTo("서울");
        assertThat(Competition.normalize("Ｓｅｏｕｌ ２０２６")).isEqualTo("seoul2026");
        assertThat(Competition.cleanDisplayName("서울\u200B 오픈")).isEqualTo("서울 오픈");
    }

    @Test
    @DisplayName("정리하면 비는 이름, 소문자 변환으로 최대 길이를 넘는 이름은 WRONG_PARAMETER")
    void of_rejectsBlankOrTooLongAfterCleanup() {
        assertThatThrownBy(() -> Competition.of("\u00A0\u3000\u200B", 2026))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
        assertThatThrownBy(() -> Competition.of("\u0130".repeat(Competition.NAME_MAX_LENGTH), 2026))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
    }

    @Test
    @DisplayName("표시용 이름은 원문 표기(호환 자모·㈜)를 유지하고 NFD 한글만 결합한다. 검색용 이름은 NFKC")
    void displayName_keepsOriginalForm() {
        assertThat(Competition.of("ㅇㅇ컵", null).getName()).isEqualTo("ㅇㅇ컵");
        assertThat(Competition.of("㈜OSS 오픈", null).getName()).isEqualTo("㈜OSS 오픈");
        assertThat(Competition.of(Normalizer.normalize("서울", Normalizer.Form.NFD), null).getName()).isEqualTo("서울");
        assertThat(Competition.of("㈜OSS 오픈", null).getSearchName()).isEqualTo("(주)oss오픈");
    }

    @Test
    @DisplayName("NUL 등 공백이 아닌 제어 문자는 지우고, 탭·줄바꿈은 공백으로 본다")
    void controlCharacters() {
        assertThat(Competition.normalize("서울\u0000오픈")).isEqualTo("서울오픈");
        assertThat(Competition.cleanDisplayName("서울\t오픈\n")).isEqualTo("서울 오픈");
    }

    // --- 대회 이벤트: 상태 파생 ---
    // 일정: 접수 시작 10/1 · 얼리버드 마감 11/8 · 접수 마감 11/15 · 대회 11/22
    private static final LocalDate START = LocalDate.of(2026, 10, 1);
    private static final LocalDate EARLY_BIRD = LocalDate.of(2026, 11, 8);
    private static final LocalDate DEADLINE = LocalDate.of(2026, 11, 15);
    private static final LocalDate OPEN_DATE = LocalDate.of(2026, 11, 22);

    private static CompetitionFee fee(String label, int amount, LocalDate until) {
        return new CompetitionFee(label, amount, until);
    }

    // 얼리버드(~EARLY_BIRD) → 기본 참가비 두 단계
    private static List<CompetitionFee> twoTiers() {
        return new ArrayList<>(List.of(fee("얼리버드", 60000, EARLY_BIRD), fee("기본 참가비", 70000, null)));
    }

    private static Competition.CompetitionBuilder event() {
        return Competition.builder()
                .name("서울 오픈").searchName("서울오픈").competitionYear(2026)
                .published(true)
                .registrationStartDate(START)
                .earlyBirdDeadline(EARLY_BIRD)
                .registrationDeadline(DEADLINE)
                .openDate(OPEN_DATE);
    }

    @Test
    @DisplayName("접수 시작 전은 UPCOMING, 시작일부터 접수 마감일(당일 포함)까지 OPEN")
    void status_upcomingAndOpen() {
        Competition competition = event().build();

        assertThat(competition.status(START.minusDays(1))).isEqualTo(CompetitionStatus.UPCOMING);
        assertThat(competition.status(START)).isEqualTo(CompetitionStatus.OPEN);
        assertThat(competition.status(DEADLINE)).isEqualTo(CompetitionStatus.OPEN);
    }

    @Test
    @DisplayName("접수 마감 다음 날부터 대회 당일까지 CLOSED, 대회 다음 날부터 ENDED")
    void status_closedAndEnded() {
        Competition competition = event().build();

        assertThat(competition.status(DEADLINE.plusDays(1))).isEqualTo(CompetitionStatus.CLOSED);
        assertThat(competition.status(OPEN_DATE)).isEqualTo(CompetitionStatus.CLOSED);
        assertThat(competition.status(OPEN_DATE.plusDays(1))).isEqualTo(CompetitionStatus.ENDED);
    }

    @Test
    @DisplayName("접수 일정이 비어 있으면 제약으로 보지 않고, 대회일만으로 종료를 판단한다")
    void status_missingSchedule() {
        Competition competition = Competition.builder()
                .name("서울 오픈").searchName("서울오픈").competitionYear(2026)
                .published(true).openDate(OPEN_DATE)
                .build();

        assertThat(competition.status(START)).isEqualTo(CompetitionStatus.OPEN);
        assertThat(competition.status(OPEN_DATE.plusDays(1))).isEqualTo(CompetitionStatus.ENDED);
    }

    @Test
    @DisplayName("미공개(카탈로그) 엔트리는 상태를 물으면 IllegalStateException")
    void status_unpublishedThrows() {
        Competition catalog = Competition.of("서울 오픈", 2026);

        assertThatThrownBy(() -> catalog.status(START)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> catalog.isEarlyBirdOpen(START)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("얼리버드는 접수 중이고 얼리버드 마감일(당일 포함)까지만 열려 있다")
    void isEarlyBirdOpen() {
        Competition competition = event().build();

        assertThat(competition.isEarlyBirdOpen(START.minusDays(1))).isFalse();  // 접수 전
        assertThat(competition.isEarlyBirdOpen(START)).isTrue();
        assertThat(competition.isEarlyBirdOpen(EARLY_BIRD)).isTrue();
        assertThat(competition.isEarlyBirdOpen(EARLY_BIRD.plusDays(1))).isFalse();
        assertThat(event().earlyBirdDeadline(null).build().isEarlyBirdOpen(START)).isFalse();
    }

    // --- 대회 이벤트: 불변식 ---

    @Test
    @DisplayName("일정·참가비가 올바르면 검증을 통과하고, 기존 카탈로그 엔트리도 통과한다")
    void validateEventInfo_valid() {
        assertThatCode(() -> event().fees(twoTiers()).build().validateEventInfo()).doesNotThrowAnyException();
        assertThatCode(() -> event().earlyBirdDeadline(null).registrationStartDate(null).build().validateEventInfo()).doesNotThrowAnyException();
        assertThatCode(() -> Competition.of("서울 오픈", null).validateEventInfo()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("공개 대회에 대회일이 없으면 COMPETITION_INVALID_EVENT_INFO")
    void validateEventInfo_publishedRequiresOpenDate() {
        assertInvalid(event().openDate(null).registrationDeadline(null).earlyBirdDeadline(null).registrationStartDate(null).build());
    }

    @Test
    @DisplayName("일정 순서가 어긋나면(비어 있는 값은 건너뛰고 비교) COMPETITION_INVALID_EVENT_INFO")
    void validateEventInfo_scheduleOrder() {
        assertInvalid(event().registrationStartDate(EARLY_BIRD.plusDays(1)).build());          // 시작 > 얼리버드
        assertInvalid(event().earlyBirdDeadline(DEADLINE.plusDays(1)).build());                 // 얼리버드 > 접수 마감
        assertInvalid(event().registrationDeadline(OPEN_DATE.plusDays(1)).build());             // 접수 마감 > 대회일
        assertInvalid(event().earlyBirdDeadline(null).registrationDeadline(null)
                .registrationStartDate(OPEN_DATE.plusDays(1)).build());                          // 사이가 비어도 시작 > 대회일
    }

    @Test
    @DisplayName("참가비 단계: 1·2차 얼리버드처럼 여러 단계, 기본 참가비만(얼리버드 마감일 유무 상관없이), 무료, 참가비 모름(빈 목록)은 통과")
    void validateEventInfo_fees_valid() {
        LocalDate first = EARLY_BIRD.minusDays(14);
        assertThatCode(() -> event().fees(new ArrayList<>(List.of(
                fee("1차 얼리버드", 50000, first), fee("2차 얼리버드", 60000, EARLY_BIRD), fee("기본 참가비", 70000, null))))
                .build().validateEventInfo()).doesNotThrowAnyException();
        assertThatCode(() -> event().earlyBirdDeadline(null).fees(new ArrayList<>(List.of(fee("참가비", 70000, null))))
                .build().validateEventInfo()).doesNotThrowAnyException();
        assertThatCode(() -> event().build().validateEventInfo()).doesNotThrowAnyException();   // 얼리버드 마감일은 알고 참가비는 모름
        assertThatCode(() -> event().fees(new ArrayList<>(List.of(fee("얼리버드", 60000, EARLY_BIRD)))).build().validateEventInfo())
                .doesNotThrowAnyException();   // 기본 참가비 미확인
        assertThatCode(() -> event().fees(new ArrayList<>(List.of(fee("참가비", 70000, null)))).build().validateEventInfo())
                .doesNotThrowAnyException();   // 얼리버드 마감일은 알지만 얼리버드 금액은 모름
        assertThatCode(() -> event().earlyBirdDeadline(null).fees(new ArrayList<>(List.of(fee("참가비", 0, null)))).build().validateEventInfo())
                .doesNotThrowAnyException();   // 무료 대회
    }

    @Test
    @DisplayName("참가비 단계: 음수 금액·빈 이름, 기한 순서 어긋남·같은 기한, 기한 없는 단계가 중간, 기한이 접수 기간 밖이면 COMPETITION_INVALID_EVENT_INFO")
    void validateEventInfo_fees_invalidTiers() {
        assertInvalid(event().fees(new ArrayList<>(List.of(fee("얼리버드", -1, EARLY_BIRD), fee("기본", 70000, null)))).build());
        assertInvalid(event().fees(new ArrayList<>(List.of(fee(" ", 60000, EARLY_BIRD), fee("기본", 70000, null)))).build());
        assertInvalid(event().fees(new ArrayList<>(List.of(fee("가".repeat(21), 60000, EARLY_BIRD), fee("기본", 70000, null)))).build());
        assertInvalid(event().fees(new ArrayList<>(List.of(fee("2차", 60000, EARLY_BIRD), fee("1차", 50000, EARLY_BIRD.minusDays(7))))).build());
        assertInvalid(event().fees(new ArrayList<>(List.of(fee("1차", 50000, EARLY_BIRD), fee("2차", 60000, EARLY_BIRD)))).build());
        assertInvalid(event().fees(new ArrayList<>(List.of(fee("기본", 70000, null), fee("얼리버드", 60000, EARLY_BIRD)))).build());
        assertInvalid(event().earlyBirdDeadline(DEADLINE)
                .fees(new ArrayList<>(List.of(fee("얼리버드", 60000, DEADLINE.plusDays(1))))).build());   // 접수 마감 뒤 (얼리버드 마감 일치 검사에서 걸림)
        assertInvalid(event().fees(new ArrayList<>(List.of(
                fee("1차", 50000, START.minusDays(1)), fee("얼리버드", 60000, EARLY_BIRD)))).build());          // 접수 시작 전
        assertInvalid(event().registrationDeadline(null).fees(new ArrayList<>(List.of(
                fee("얼리버드", 60000, EARLY_BIRD), fee("2차", 65000, OPEN_DATE.plusDays(1))))).build());      // 접수 마감 없으면 대회일 뒤 (같음)
    }

    @Test
    @DisplayName("얼리버드 기준은 일정 하나: 기한 있는 단계가 있는데 얼리버드 마감일이 없거나, 마감일이 마지막 할인 단계 기한과 다르면 COMPETITION_INVALID_EVENT_INFO")
    void validateEventInfo_fees_earlyBirdDeadlineIsSingleSource() {
        assertInvalid(event().earlyBirdDeadline(null).fees(twoTiers()).build());
        assertInvalid(event().earlyBirdDeadline(EARLY_BIRD.minusDays(1)).fees(twoTiers()).build());
        assertInvalid(event().earlyBirdDeadline(EARLY_BIRD.minusDays(14)).fees(new ArrayList<>(List.of(
                fee("1차 얼리버드", 50000, EARLY_BIRD.minusDays(14)), fee("2차 얼리버드", 60000, EARLY_BIRD), fee("기본", 70000, null)))).build());   // 1차 날짜 = 2차 할인 중에 얼리버드 끝난 것으로 보임
        assertInvalid(event().earlyBirdDeadline(null)
                .fees(new ArrayList<>(List.of(fee("참가비", 70000, DEADLINE)))).build());   // 기본 참가비에 기한을 넣으면 할인 단계로 봄 → 얼리버드 마감일 필요
    }

    @Test
    @DisplayName("참가비 단계는 밖에서 바꿀 수 없다")
    void fees_unmodifiable() {
        Competition competition = event().fees(twoTiers()).build();

        assertThat(competition.getFees()).extracting(CompetitionFee::getLabel).containsExactly("얼리버드", "기본 참가비");
        assertThatThrownBy(() -> competition.getFees().clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("종목 컬렉션은 밖에서 바꿀 수 없다")
    void uniforms_unmodifiable() {
        Competition competition = event().uniforms(new LinkedHashSet<>(Set.of(CompetitionUniform.GI))).build();

        assertThat(competition.getUniforms()).containsExactly(CompetitionUniform.GI);
        assertThatThrownBy(() -> competition.getUniforms().add(CompetitionUniform.NO_GI))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static void assertInvalid(Competition competition) {
        assertThatThrownBy(competition::validateEventInfo)
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
    }

    private static Competition.CompetitionBuilder filledForPublish() {
        return Competition.builder()
                .name("서울 오픈")
                .searchName("서울오픈")
                .competitionYear(2026)
                .published(false)
                .openDate(LocalDate.of(2026, 12, 6))
                .startTime(LocalTime.of(9, 0))
                .region(CompetitionRegion.SEOUL)
                .uniforms(new LinkedHashSet<>(Set.of(CompetitionUniform.GI)))
                .registrationStartDate(LocalDate.of(2026, 11, 1))
                .registrationDeadline(LocalDate.of(2026, 11, 30))
                .noticeUrl("https://example.com/notice")
                .lastVerifiedDate(LocalDate.of(2026, 11, 13));
    }

    @Test
    @DisplayName("missingRequiredFields: 공개 필수 칸이 다 채워졌으면 빈 목록")
    void missingRequiredFields_allFilled() {
        assertThat(filledForPublish().build().missingRequiredFields()).isEmpty();
    }

    @Test
    @DisplayName("missingRequiredFields: 시작 시각·접수 시작이 빈 대회는 그 칸만 (dev 시드 대회 모양)")
    void missingRequiredFields_onlyEmptyOnes() {
        Competition competition = filledForPublish()
                .startTime(null)
                .registrationStartDate(null)
                .build();

        assertThat(competition.missingRequiredFields())
                .containsExactly(CompetitionRequiredField.START_TIME, CompetitionRequiredField.REGISTRATION_START_DATE);
    }

    @Test
    @DisplayName("missingRequiredFields: 공지 링크가 공백뿐이면 빈 칸, 종목이 비어 있으면 빈 칸")
    void missingRequiredFields_blankUrlAndNoUniforms() {
        Competition competition = filledForPublish()
                .noticeUrl("  ")
                .uniforms(new LinkedHashSet<>())
                .build();

        assertThat(competition.missingRequiredFields())
                .containsExactly(CompetitionRequiredField.UNIFORMS, CompetitionRequiredField.NOTICE_URL);
    }

    @Test
    @DisplayName("missingRequiredFields: 자동완성용 대회(이름·연도만)는 모든 칸이 선언 순서대로")
    void missingRequiredFields_catalogEntry() {
        assertThat(Competition.of("서울 오픈", 2025).missingRequiredFields())
                .containsExactly(CompetitionRequiredField.values());
    }

    private static CompetitionEventInfo eventInfo(LocalDate openDate, LocalDate earlyBird, List<CompetitionFee> fees) {
        return new CompetitionEventInfo(openDate, LocalTime.of(10, 0), CompetitionRegion.BUSAN, "부산", "사직체육관", "부산협회",
                "https://example.com/busan", new LinkedHashSet<>(Set.of(CompetitionUniform.NO_GI)), CompetitionAudience.ADULT,
                LocalDate.of(2026, 11, 1), earlyBird, LocalDate.of(2026, 11, 30), new ArrayList<>(fees), "성인 기준");
    }

    @Test
    @DisplayName("changeEventInfo: 칸을 전부 바꾸고 종목·참가비는 새 값으로 교체한다")
    void changeEventInfo_replacesAll() {
        Competition competition = filledForPublish()
                .uniforms(new LinkedHashSet<>(Set.of(CompetitionUniform.GI)))
                .fees(new ArrayList<>(List.of(new CompetitionFee("기본", 50000, null))))
                .build();

        competition.changeEventInfo(eventInfo(LocalDate.of(2026, 12, 13), LocalDate.of(2026, 11, 20),
                List.of(new CompetitionFee("얼리버드", 40000, LocalDate.of(2026, 11, 20)), new CompetitionFee("기본", 60000, null))));

        assertThat(competition.getOpenDate()).isEqualTo(LocalDate.of(2026, 12, 13));
        assertThat(competition.getVenue()).isEqualTo("사직체육관");
        assertThat(competition.getUniforms()).containsExactly(CompetitionUniform.NO_GI);
        assertThat(competition.getFees()).extracting(CompetitionFee::getLabel).containsExactly("얼리버드", "기본");
        assertThat(competition.getEarlyBirdDeadline()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(competition.getLastVerifiedDate()).isEqualTo(LocalDate.of(2026, 11, 13));   // 마지막 확인일은 그대로
    }

    @Test
    @DisplayName("changeEventInfo: null·빈 목록이면 그 칸을 비운다")
    void changeEventInfo_clears() {
        Competition competition = filledForPublish()
                .fees(new ArrayList<>(List.of(new CompetitionFee("기본", 50000, null))))
                .build();

        competition.changeEventInfo(new CompetitionEventInfo(LocalDate.of(2026, 12, 6), null, null, null, null, null, null,
                new LinkedHashSet<>(), null, null, null, null, new ArrayList<>(), null));

        assertThat(competition.getStartTime()).isNull();
        assertThat(competition.getRegion()).isNull();
        assertThat(competition.getNoticeUrl()).isNull();
        assertThat(competition.getUniforms()).isEmpty();
        assertThat(competition.getFees()).isEmpty();
        assertThat(competition.getRegistrationDeadline()).isNull();
    }

    @Test
    @DisplayName("changeEventInfo: 일정 순서가 틀리면 COMPETITION_INVALID_EVENT_INFO")
    void changeEventInfo_invalidSchedule() {
        Competition competition = filledForPublish().build();

        // 접수 마감(11/30)이 대회일(11/20)보다 뒤
        assertThatThrownBy(() -> competition.changeEventInfo(eventInfo(LocalDate.of(2026, 11, 20), null, List.of())))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
    }

    @Test
    @DisplayName("changeEventInfo: 할인 단계 기한과 얼리버드 마감이 다르면 COMPETITION_INVALID_EVENT_INFO (같이 바꾸면 통과)")
    void changeEventInfo_feesMustMatchEarlyBird() {
        Competition competition = filledForPublish().build();
        List<CompetitionFee> fees = List.of(new CompetitionFee("얼리버드", 40000, LocalDate.of(2026, 11, 20)), new CompetitionFee("기본", 60000, null));

        assertThatThrownBy(() -> competition.changeEventInfo(eventInfo(LocalDate.of(2026, 12, 6), LocalDate.of(2026, 11, 15), fees)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
        assertThatCode(() -> competition.changeEventInfo(eventInfo(LocalDate.of(2026, 12, 6), LocalDate.of(2026, 11, 20), fees)))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("rename: 표시·검색 이름과 연도를 바꾸고, 정리하면 비는 이름은 WRONG_PARAMETER")
    void rename() {
        Competition competition = Competition.of("서울 오픈", 2026);

        competition.rename("  Seoul   Open ", 2027);

        assertThat(competition.getName()).isEqualTo("Seoul Open");
        assertThat(competition.getSearchName()).isEqualTo("seoulopen");
        assertThat(competition.getCompetitionYear()).isEqualTo(2027);
        assertThatThrownBy(() -> competition.rename("\u00A0", 2026))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
    }

    @Test
    @DisplayName("validateEventInfo: 대회일과 연도가 둘 다 있으면 연도 = 대회일 연도 (연도 없는 자동완성용·대회일 없는 대회는 보지 않음)")
    void validateEventInfo_yearMatchesOpenDate() {
        assertThatThrownBy(() -> filledForPublish().competitionYear(2025).build().validateEventInfo())
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_INVALID_EVENT_INFO);
        assertThatCode(() -> filledForPublish().competitionYear(null).build().validateEventInfo()).doesNotThrowAnyException();
        assertThatCode(() -> Competition.of("작년 대회", 2025).validateEventInfo()).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("publish: 마지막 확인일을 뺀 필수 칸이 다 있으면 공개하고 확인일을 오늘로 (확인일이 비어 있어도)")
    void publish_setsVerifiedDate() {
        Competition competition = filledForPublish().lastVerifiedDate(null).build();

        competition.publish(LocalDate.of(2026, 11, 20));

        assertThat(competition.isPublished()).isTrue();
        assertThat(competition.getLastVerifiedDate()).isEqualTo(LocalDate.of(2026, 11, 20));
    }

    @Test
    @DisplayName("publish: 필수 칸이 비면 COMPETITION_REQUIRED_FIELDS_MISSING 이고 그대로 미공개")
    void publish_missingFields() {
        Competition competition = filledForPublish().startTime(null).build();

        assertThatThrownBy(() -> competition.publish(LocalDate.of(2026, 11, 20)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.COMPETITION_REQUIRED_FIELDS_MISSING);
        assertThat(competition.isPublished()).isFalse();
        assertThat(competition.getLastVerifiedDate()).isEqualTo(LocalDate.of(2026, 11, 13));
    }

    @Test
    @DisplayName("publish: 이미 공개면 빈 칸이 있어도 그대로, 확인일도 그대로")
    void publish_alreadyPublished() {
        Competition competition = filledForPublish().published(true).startTime(null).build();

        competition.publish(LocalDate.of(2026, 11, 20));

        assertThat(competition.isPublished()).isTrue();
        assertThat(competition.getLastVerifiedDate()).isEqualTo(LocalDate.of(2026, 11, 13));
    }

    @Test
    @DisplayName("unpublish·verify: 공개만 내리고, 확인일만 바꾼다")
    void unpublishAndVerify() {
        Competition competition = filledForPublish().published(true).build();

        competition.unpublish();
        competition.verify(LocalDate.of(2026, 11, 20));

        assertThat(competition.isPublished()).isFalse();
        assertThat(competition.getLastVerifiedDate()).isEqualTo(LocalDate.of(2026, 11, 20));
        assertThat(competition.getOpenDate()).isEqualTo(LocalDate.of(2026, 12, 6));
    }
}

