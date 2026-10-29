package com.jiujitsu.api.domain.community.profile.mapper;

import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileResponse;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDivisionDto;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDto;
import com.jiujitsu.api.domain.community.profile.dto.MedalCountDto;
import com.jiujitsu.api.domain.community.profile.entity.BeltRank;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionInfo;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionRank;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionUniform;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jiujitsu.api.domain.community.profile.entity.CompetitionRank.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * 대회 기록 응답 — 한 줄(부문)씩 저장된 기록을 대회 단위로 묶고 정렬·메달 합계를 낸다.
 */
class CommunityProfileMapperTest {

    private final CommunityProfileMapper mapper = new CommunityProfileMapper();

    private CommunityProfileResponse respond(CompetitionInfo... rows) {
        CommunityProfile profile = CommunityProfile.builder()
                .user(User.builder().nickname("tester").role(UserRole.USER).build())
                .competitions(new ArrayList<>(List.of(rows)))
                .build();
        return mapper.getCommunityProfileResponse(profile);
    }

    private CompetitionInfo row(Integer year, String name, CompetitionRank rank) {
        return CompetitionInfo.builder().competitionYear(year).competitionName(name).competitionRank(rank).build();
    }

    @Test
    @DisplayName("같은 연도·대회명의 부문 2개는 한 대회로 묶이고 부문 정보가 유지된다")
    void groupDivisionsOfSameCompetition() {
        CommunityProfileResponse response = respond(
                CompetitionInfo.builder().competitionYear(2025).competitionName("서울 오픈")
                        .competitionRank(GOLD).uniform(CompetitionUniform.GI).belt(BeltRank.BLUE).build(),
                CompetitionInfo.builder().competitionYear(2025).competitionName("서울 오픈")
                        .competitionRank(GOLD).uniform(CompetitionUniform.NO_GI).build()
        );

        assertThat(response.competitions()).hasSize(1);
        CompetitionDto competition = response.competitions().get(0);
        assertThat(competition.competitionYear()).isEqualTo(2025);
        assertThat(competition.competitionName()).isEqualTo("서울 오픈");
        assertThat(competition.divisions())
                .extracting(CompetitionDivisionDto::competitionRank, CompetitionDivisionDto::uniform, CompetitionDivisionDto::belt)
                .containsExactly(
                        tuple(GOLD, CompetitionUniform.GI, BeltRank.BLUE),
                        tuple(GOLD, CompetitionUniform.NO_GI, null)
                );
    }

    @Test
    @DisplayName("기존에 두 줄로 입력된 같은 대회는 대회명 앞뒤 공백이 달라도 한 대회로 묶인다")
    void groupLegacyDuplicateRows() {
        CommunityProfileResponse response = respond(
                row(2024, "서울 오픈", SILVER),
                row(2024, " 서울 오픈 ", GOLD)
        );

        assertThat(response.competitions())
                .extracting(CompetitionDto::competitionName)
                .containsExactly("서울 오픈");
        assertThat(response.competitions().get(0).divisions())
                .extracting(CompetitionDivisionDto::competitionRank)
                .containsExactly(GOLD, SILVER);
    }

    @Test
    @DisplayName("대회명이 같아도 연도가 다르면 다른 대회다")
    void separateByYear() {
        CommunityProfileResponse response = respond(
                row(2024, "서울 오픈", GOLD),
                row(2025, "서울 오픈", GOLD)
        );

        assertThat(response.competitions())
                .extracting(CompetitionDto::competitionYear)
                .containsExactly(2025, 2024);
    }

    @Test
    @DisplayName("연도 최신순 → 같은 연도는 최고 메달 순 → 대회명 순, 연도 없는 기록은 마지막")
    void sortCompetitions() {
        CommunityProfileResponse response = respond(
                row(null, "연도 없음", GOLD),
                row(2023, "옛 대회", GOLD),
                row(2025, "B 대회", BRONZE),
                row(2025, "C 대회", PARTICIPATION),
                row(2025, "C 대회", SILVER),
                row(2025, "A 대회", BRONZE)
        );

        assertThat(response.competitions())
                .extracting(CompetitionDto::competitionYear, CompetitionDto::competitionName)
                .containsExactly(
                        tuple(2025, "C 대회"),
                        tuple(2025, "A 대회"),
                        tuple(2025, "B 대회"),
                        tuple(2023, "옛 대회"),
                        tuple(null, "연도 없음")
                );
        assertThat(response.competitions().get(0).divisions())
                .extracting(CompetitionDivisionDto::competitionRank)
                .containsExactly(SILVER, PARTICIPATION);
    }

    @Test
    @DisplayName("메달 합계는 부문 기준으로 세고 참가는 제외한다")
    void countMedalsByDivision() {
        CommunityProfileResponse response = respond(
                row(2025, "서울 오픈", GOLD),
                row(2025, "서울 오픈", GOLD),
                row(2024, "부산 오픈", SILVER),
                row(2024, "대구 오픈", PARTICIPATION)
        );

        assertThat(response.medalCount()).isEqualTo(new MedalCountDto(2, 1, 0));
    }

    @Test
    @DisplayName("대회 기록이 없으면 빈 목록과 0개 합계")
    void emptyCompetitions() {
        CommunityProfileResponse response = respond();

        assertThat(response.competitions()).isEmpty();
        assertThat(response.medalCount()).isEqualTo(new MedalCountDto(0, 0, 0));
    }
}
