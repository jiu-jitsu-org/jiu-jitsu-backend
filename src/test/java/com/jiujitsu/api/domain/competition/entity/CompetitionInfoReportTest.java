package com.jiujitsu.api.domain.competition.entity;

import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CompetitionInfoReportTest {

    private final Competition competition = Competition.builder().id(1L).name("서울 오픈").build();
    private final User user = User.builder().id(2L).build();

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   "})
    @DisplayName("다른 정보(OTHER)는 내용이 없으면 WRONG_PARAMETER")
    void other_requiresDetail(String detail) {
        assertThatThrownBy(() -> CompetitionInfoReport.of(competition, user, CompetitionInfoField.OTHER, detail))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
    }

    @ParameterizedTest
    @EnumSource(value = CompetitionInfoField.class, names = "OTHER", mode = EnumSource.Mode.EXCLUDE)
    @DisplayName("다른 항목은 내용 없이 제보할 수 있다 (공백만 있으면 null)")
    void otherFields_detailOptional(CompetitionInfoField field) {
        CompetitionInfoReport report = CompetitionInfoReport.of(competition, user, field, "  ");

        assertThat(report.getField()).isEqualTo(field);
        assertThat(report.getDetail()).isNull();
        assertThat(report.getCompetition()).isSameAs(competition);
        assertThat(report.getUser()).isSameAs(user);
    }

    @Test
    @DisplayName("내용은 앞뒤 공백을 지워 저장한다")
    void detail_stripped() {
        CompetitionInfoReport report = CompetitionInfoReport.of(competition, user, CompetitionInfoField.REGISTRATION, "  마감 11.3  ");

        assertThat(report.getDetail()).isEqualTo("마감 11.3");
    }

    @Test
    @DisplayName("항목이 없거나 내용이 200자를 넘으면 WRONG_PARAMETER")
    void invalid() {
        assertThatThrownBy(() -> CompetitionInfoReport.of(competition, user, null, "내용"))
                .isInstanceOf(ErrorException.class);
        assertThatThrownBy(() -> CompetitionInfoReport.of(competition, user, CompetitionInfoField.FEE, "가".repeat(201)))
                .isInstanceOf(ErrorException.class);
        assertThat(CompetitionInfoReport.of(competition, user, CompetitionInfoField.FEE, "가".repeat(200)).getDetail()).hasSize(200);
    }
}
