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

import java.time.LocalDateTime;

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

    @Test
    @DisplayName("resolve: 처음 처리 시각을 남기고, 다시 처리해도 시각은 그대로. 메모는 null 이면 그대로 · 공백이면 지움 · 앞뒤 공백 정리, unresolve 는 둘 다 지움")
    void resolve() {
        CompetitionInfoReport report = CompetitionInfoReport.of(
                Competition.of("서울 오픈", 2026), null, CompetitionInfoField.FEE, null);
        LocalDateTime first = LocalDateTime.of(2026, 11, 13, 10, 0);

        report.resolve(first, " 참가비 고침 ");
        assertThat(report.getResolvedAt()).isEqualTo(first);
        assertThat(report.getResolutionNote()).isEqualTo("참가비 고침");

        report.resolve(first.plusHours(1), null);
        assertThat(report.getResolvedAt()).isEqualTo(first);
        assertThat(report.getResolutionNote()).isEqualTo("참가비 고침");

        report.resolve(first.plusHours(2), "  ");
        assertThat(report.getResolvedAt()).isEqualTo(first);
        assertThat(report.getResolutionNote()).isNull();

        report.unresolve();
        assertThat(report.getResolvedAt()).isNull();
        assertThat(report.getResolutionNote()).isNull();
    }

    @Test
    @DisplayName("resolve: 메모는 앞뒤 공백을 지운 뒤 100자까지, 넘으면 WRONG_PARAMETER 이고 처리되지 않는다")
    void resolve_noteTooLong() {
        CompetitionInfoReport report = CompetitionInfoReport.of(
                Competition.of("서울 오픈", 2026), null, CompetitionInfoField.FEE, null);
        LocalDateTime now = LocalDateTime.of(2026, 11, 13, 10, 0);

        assertThatThrownBy(() -> report.resolve(now, "가".repeat(101)))
                .isInstanceOf(ErrorException.class)
                .extracting("errorCode").isEqualTo(ErrorCode.WRONG_PARAMETER);
        assertThat(report.getResolvedAt()).isNull();

        report.resolve(now, " " + "가".repeat(100) + "\n");
        assertThat(report.getResolutionNote()).hasSize(100);
    }
}
