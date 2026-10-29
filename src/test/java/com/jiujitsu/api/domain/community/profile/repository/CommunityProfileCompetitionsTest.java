package com.jiujitsu.api.domain.community.profile.repository;

import com.jiujitsu.api.domain.community.profile.dto.CompetitionDivisionDto;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDto;
import com.jiujitsu.api.domain.community.profile.entity.*;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.global.security.AuditorAwareImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;

/**
 * 대회 정보 저장(upsertCompetitions → save) 검증.
 * 서비스와 같은 순서(조회 → 대회 목록 교체 → save)로 저장하고, 대회·부문이 그대로 저장·교체되는지 본다.
 */
@DataJpaTest
@Import(AuditorAwareImpl.class)
class CommunityProfileCompetitionsTest {

    @Autowired
    private CommunityProfileRepository communityProfileRepository;

    @Autowired
    private TestEntityManager em;

    private User saveUser() {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .ownerRequested(false)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private CompetitionDto competition(String name, CompetitionRank rank) {
        return new CompetitionDto(2021, name, null, null,
                List.of(new CompetitionDivisionDto(rank, null, null, null, null, null)));
    }

    private List<CompetitionRecord> reloadCompetitions(User user) {
        em.flush();
        em.clear();
        return communityProfileRepository.findByUser(user).orElseThrow().getCompetitions();
    }

    private long countRows(String table) {
        return ((Number) em.getEntityManager().createNativeQuery("select count(*) from " + table).getSingleResult()).longValue();
    }

    @Test
    @DisplayName("이미 저장된 프로필에 대회 정보를 추가해도 저장된다")
    void addCompetitionToExistingProfile() {
        User user = saveUser();
        em.persist(new CommunityProfile(user));
        em.flush();
        em.clear();

        CommunityProfile profile = communityProfileRepository.findByUser(user).orElseThrow();
        profile.upsertCompetitions(List.of(competition("오오", CompetitionRank.PARTICIPATION)));
        communityProfileRepository.save(profile);

        assertThat(reloadCompetitions(user))
                .extracting(CompetitionRecord::getCompetitionName)
                .containsExactly("오오");
    }

    @Test
    @DisplayName("대회·부문의 모든 값이 저장되고, 대회명은 앞뒤 공백을 빼고 없으면 빈 값으로 저장된다")
    void saveAllFields() {
        User user = saveUser();
        CommunityProfile profile = new CommunityProfile(user);
        profile.upsertCompetitions(List.of(
                new CompetitionDto(2025, "  서울 오픈 ", true, 0, List.of(
                        new CompetitionDivisionDto(CompetitionRank.GOLD, CompetitionUniform.GI, BeltRank.BLUE,
                                76.5, CompetitionWeightBound.UNDER, false),
                        new CompetitionDivisionDto(CompetitionRank.GOLD, CompetitionUniform.NO_GI, null,
                                null, null, true))),
                new CompetitionDto(2024, null, null, null, List.of(
                        new CompetitionDivisionDto(CompetitionRank.SILVER, null, null, null, null, null)))
        ));
        communityProfileRepository.save(profile);

        List<CompetitionRecord> records = reloadCompetitions(user);
        assertThat(records)
                .extracting(CompetitionRecord::getCompetitionYear, CompetitionRecord::getCompetitionName,
                        CompetitionRecord::isCompetitionPinned, CompetitionRecord::getCompetitionDisplayOrder)
                .containsExactlyInAnyOrder(
                        tuple(2025, "서울 오픈", true, 0),
                        tuple(2024, "", false, null)
                );
        CompetitionRecord seoul = records.stream().filter(CompetitionRecord::isNamed).findFirst().orElseThrow();
        assertThat(seoul.getDivisions())
                .extracting(CompetitionDivision::getCompetitionRank, CompetitionDivision::getCompetitionUniform,
                        CompetitionDivision::getCompetitionBelt, CompetitionDivision::getCompetitionWeightLimit,
                        CompetitionDivision::getCompetitionWeightBound, CompetitionDivision::isCompetitionAbsolute)
                .containsExactlyInAnyOrder(
                        tuple(CompetitionRank.GOLD, CompetitionUniform.GI, BeltRank.BLUE, 76.5, CompetitionWeightBound.UNDER, false),
                        tuple(CompetitionRank.GOLD, CompetitionUniform.NO_GI, null, null, null, true)
                );
    }

    @Test
    @DisplayName("다시 저장하면 이전 대회·부문 행이 지워지고 대체되며, 빈 목록이면 모두 지운다")
    void replaceAndClearCompetitions() {
        User user = saveUser();
        CommunityProfile created = new CommunityProfile(user);
        created.upsertCompetitions(List.of(competition("A", CompetitionRank.GOLD)));
        communityProfileRepository.save(created);
        em.flush();
        em.clear();

        CommunityProfile profile = communityProfileRepository.findByUser(user).orElseThrow();
        profile.upsertCompetitions(List.of(
                competition("B", CompetitionRank.SILVER),
                competition("C", CompetitionRank.BRONZE)
        ));
        communityProfileRepository.save(profile);
        assertThat(reloadCompetitions(user))
                .extracting(CompetitionRecord::getCompetitionName)
                .containsExactlyInAnyOrder("B", "C");
        assertThat(countRows("community_profile_competition_records")).isEqualTo(2);
        assertThat(countRows("community_profile_competition_divisions")).isEqualTo(2);

        CommunityProfile again = communityProfileRepository.findByUser(user).orElseThrow();
        again.upsertCompetitions(List.of());
        communityProfileRepository.save(again);
        assertThat(reloadCompetitions(user)).isEmpty();
        assertThat(countRows("community_profile_competition_records")).isZero();
        assertThat(countRows("community_profile_competition_divisions")).isZero();
    }
}
