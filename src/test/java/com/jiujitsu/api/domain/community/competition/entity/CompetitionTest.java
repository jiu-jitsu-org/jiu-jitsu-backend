package com.jiujitsu.api.domain.community.competition.entity;

import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.text.Normalizer;

import static org.assertj.core.api.Assertions.assertThat;
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
}
