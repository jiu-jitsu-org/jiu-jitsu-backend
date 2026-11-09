package com.jiujitsu.api.domain.community.competition.entity;

import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;
import java.time.LocalDate;
import java.util.LinkedHashSet;
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
        assertThatCode(() -> event().earlyBirdFee(60000).baseFee(70000).build().validateEventInfo()).doesNotThrowAnyException();
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
    @DisplayName("참가비가 음수이거나, 얼리버드 금액만 있고 얼리버드 마감일이 없으면 COMPETITION_INVALID_EVENT_INFO")
    void validateEventInfo_fees() {
        assertInvalid(event().baseFee(-1).build());
        assertInvalid(event().earlyBirdFee(-1).build());
        assertInvalid(event().earlyBirdDeadline(null).earlyBirdFee(60000).build());
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
}
