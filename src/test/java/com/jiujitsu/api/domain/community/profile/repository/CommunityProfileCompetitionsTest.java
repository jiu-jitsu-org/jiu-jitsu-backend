package com.jiujitsu.api.domain.community.profile.repository;

import com.jiujitsu.api.domain.community.profile.dto.CompetitionDivisionDto;
import com.jiujitsu.api.domain.community.profile.dto.CompetitionDto;
import com.jiujitsu.api.domain.community.profile.entity.BeltRank;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionInfo;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionRank;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionUniform;
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
 * 서비스와 같은 순서(조회 → 대회 목록 교체 → save)로 저장하고, 부문마다 한 줄씩 저장되는지 본다.
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

    private CompetitionDto competition(String name, CompetitionRank... ranks) {
        return new CompetitionDto(2021, name, java.util.Arrays.stream(ranks)
                .map(rank -> new CompetitionDivisionDto(rank, null, null))
                .toList());
    }

    private List<CompetitionInfo> reloadCompetitions(User user) {
        em.flush();
        em.clear();
        return communityProfileRepository.findByUser(user).orElseThrow().getCompetitions();
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
                .extracting(CompetitionInfo::getCompetitionName, CompetitionInfo::getCompetitionRank)
                .containsExactly(tuple("오오", CompetitionRank.PARTICIPATION));
    }

    @Test
    @DisplayName("대회 정보를 다시 저장하면 이전 목록을 대체하고, 빈 목록이면 모두 지운다")
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
                .extracting(CompetitionInfo::getCompetitionName)
                .containsExactlyInAnyOrder("B", "C");

        CommunityProfile again = communityProfileRepository.findByUser(user).orElseThrow();
        again.upsertCompetitions(List.of());
        communityProfileRepository.save(again);
        assertThat(reloadCompetitions(user)).isEmpty();
    }

    @Test
    @DisplayName("한 대회의 부문들은 연도·대회명·부문 정보를 가진 한 줄씩으로 저장된다")
    void saveDivisionsAsRows() {
        User user = saveUser();
        CommunityProfile profile = new CommunityProfile(user);
        profile.upsertCompetitions(List.of(new CompetitionDto(2025, "  서울 오픈 ", List.of(
                new CompetitionDivisionDto(CompetitionRank.GOLD, CompetitionUniform.GI, BeltRank.BLUE),
                new CompetitionDivisionDto(CompetitionRank.SILVER, CompetitionUniform.NO_GI, null)
        ))));
        communityProfileRepository.save(profile);

        assertThat(reloadCompetitions(user))
                .extracting(CompetitionInfo::getCompetitionYear, CompetitionInfo::getCompetitionName,
                        CompetitionInfo::getCompetitionRank, CompetitionInfo::getUniform, CompetitionInfo::getBelt)
                .containsExactlyInAnyOrder(
                        tuple(2025, "서울 오픈", CompetitionRank.GOLD, CompetitionUniform.GI, BeltRank.BLUE),
                        tuple(2025, "서울 오픈", CompetitionRank.SILVER, CompetitionUniform.NO_GI, null)
                );
    }
}
