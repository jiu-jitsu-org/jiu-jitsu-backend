package com.jiujitsu.api.domain.community.profile.mapper;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDivisionDto;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDto;
import com.jiujitsu.api.domain.community.profile.dto.MedalCountDto;
import com.jiujitsu.api.domain.community.profile.entity.*;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.jiujitsu.api.domain.community.profile.entity.CompetitionRank.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * 대회 기록 응답 — 서버 정렬(고정 → 순서 → 연도 → 메달 → 이름), 부문 정렬, 메달 합계.
 */
class CommunityProfileMapperTest {

    private final CommunityProfileMapper mapper = new CommunityProfileMapper();

    private CommunityProfileResponse respond(CompetitionRecord... records) {
        CommunityProfile profile = CommunityProfile.builder()
                .user(User.builder().nickname("tester").role(UserRole.USER).build())
                .competitions(new ArrayList<>(List.of(records)))
                .build();
        return mapper.getCommunityProfileResponse(profile);
    }

    private CompetitionRecord record(Integer year, String name, boolean pinned, Integer order, CompetitionRank... ranks) {
        return CompetitionRecord.builder()
                .competitionYear(year)
                .competitionName(name)
                .competitionPinned(pinned)
                .competitionDisplayOrder(order)
                .divisions(new ArrayList<>(Arrays.stream(ranks)
                        .map(rank -> CompetitionDivision.builder().competitionRank(rank).build())
                        .toList()))
                .build();
    }

    private CompetitionRecord record(Integer year, String name, CompetitionRank... ranks) {
        return record(year, name, false, null, ranks);
    }

    @Test
    @DisplayName("고정한 대회가 먼저, 각 묶음 안에서는 회원이 정한 순서 → 연도 최신순 → 최고 메달 → 대회명")
    void sortCompetitions() {
        CommunityProfileResponse response = respond(
                record(2025, "B 대회", BRONZE),
                record(2025, "A 대회", BRONZE),
                record(2025, "C 대회", PARTICIPATION, SILVER),
                record(2023, "옛 대회", GOLD),
                record(2020, "순서 1", false, 1, GOLD),
                record(2019, "순서 0", false, 0, GOLD),
                record(2018, "고정 순서 1", true, 1, GOLD),
                record(2017, "고정 순서 0", true, 0, BRONZE)
        );

        assertThat(response.competitions())
                .extracting(CompetitionDto::competitionName)
                .containsExactly("고정 순서 0", "고정 순서 1", "순서 0", "순서 1", "C 대회", "A 대회", "B 대회", "옛 대회");
    }

    @Test
    @DisplayName("부문은 메달 → 도복/노기 → 띠 순이고 모든 부문 값이 내려간다")
    void sortDivisions() {
        CompetitionRecord seoul = CompetitionRecord.builder()
                .competitionYear(2025).competitionName("서울 오픈")
                .divisions(new ArrayList<>(List.of(
                        CompetitionDivision.builder().competitionRank(PARTICIPATION).build(),
                        CompetitionDivision.builder().competitionRank(GOLD).competitionUniform(CompetitionUniform.NO_GI)
                                .competitionAbsolute(true).build(),
                        CompetitionDivision.builder().competitionRank(GOLD).competitionUniform(CompetitionUniform.GI)
                                .competitionBelt(BeltRank.BLUE).competitionWeightLimit(76.5)
                                .competitionWeightBound(CompetitionWeightBound.UNDER).build()
                )))
                .build();

        CompetitionDto competition = respond(seoul).competitions().get(0);

        assertThat(competition.competitionPinned()).isFalse();
        assertThat(competition.divisions())
                .extracting(CompetitionDivisionDto::competitionRank, CompetitionDivisionDto::competitionUniform,
                        CompetitionDivisionDto::competitionBelt, CompetitionDivisionDto::competitionWeightLimit,
                        CompetitionDivisionDto::competitionWeightBound, CompetitionDivisionDto::competitionAbsolute)
                .containsExactly(
                        tuple(GOLD, CompetitionUniform.GI, BeltRank.BLUE, 76.5, CompetitionWeightBound.UNDER, false),
                        tuple(GOLD, CompetitionUniform.NO_GI, null, null, null, true),
                        tuple(PARTICIPATION, null, null, null, null, false)
                );
    }

    @Test
    @DisplayName("메달 합계는 부문 기준, 참가와 대회명 없는 기록은 제외한다")
    void countMedals() {
        CommunityProfileResponse response = respond(
                record(2025, "서울 오픈", GOLD, GOLD, PARTICIPATION),
                record(2024, "부산 오픈", SILVER),
                record(2024, "", BRONZE)
        );

        assertThat(response.medalCount()).isEqualTo(new MedalCountDto(2, 1, 0));
    }

    @Test
    @DisplayName("대회명 없는 기록도 내 프로필에는 빈 대회명으로 내려간다")
    void namelessRecordInMyProfile() {
        CommunityProfileResponse response = respond(record(2024, "", BRONZE));

        assertThat(response.competitions())
                .extracting(CompetitionDto::competitionYear, CompetitionDto::competitionName)
                .containsExactly(tuple(2024, ""));
    }

    @Test
    @DisplayName("대회 기록이 없으면 빈 목록과 0개 합계")
    void emptyCompetitions() {
        CommunityProfileResponse response = respond();

        assertThat(response.competitions()).isEmpty();
        assertThat(response.medalCount()).isEqualTo(new MedalCountDto(0, 0, 0));
    }

    @Test
    @DisplayName("프로필이 없는 사용자도 대회는 빈 목록·0개 합계로 내려간다")
    void userWithoutProfile() {
        CommunityProfileResponse response = mapper.getCommunityProfileResponseUser(
                User.builder().nickname("tester").role(UserRole.USER).build());

        assertThat(response.competitions()).isEmpty();
        assertThat(response.medalCount()).isEqualTo(new MedalCountDto(0, 0, 0));
    }

    @Test
    @DisplayName("메달·도복·띠가 같은 부문은 체급 → 앱솔루트 순으로, 입력 순서와 상관없이 같은 순서로 내려간다")
    void tieBreakDivisions() {
        CompetitionDivision weight = CompetitionDivision.builder().competitionRank(GOLD)
                .competitionUniform(CompetitionUniform.GI).competitionBelt(BeltRank.BLUE)
                .competitionWeightLimit(76.0).competitionWeightBound(CompetitionWeightBound.UNDER).build();
        CompetitionDivision absolute = CompetitionDivision.builder().competitionRank(GOLD)
                .competitionUniform(CompetitionUniform.GI).competitionBelt(BeltRank.BLUE)
                .competitionAbsolute(true).build();

        for (List<CompetitionDivision> input : List.of(List.of(weight, absolute), List.of(absolute, weight))) {
            CompetitionRecord seoul = CompetitionRecord.builder()
                    .competitionYear(2025).competitionName("서울 오픈")
                    .divisions(new ArrayList<>(input))
                    .build();

            assertThat(respond(seoul).competitions().get(0).divisions())
                    .extracting(CompetitionDivisionDto::competitionAbsolute)
                    .containsExactly(false, true);
        }
    }
}
