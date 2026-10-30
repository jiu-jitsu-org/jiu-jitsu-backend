package com.jiujitsu.api.domain.community.profile.repository;

import com.jiujitsu.api.domain.community.profile.entity.BeltRank;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommunityProfileRepositoryTest {

    @Autowired
    private CommunityProfileRepository communityProfileRepository;

    @Autowired
    private TestEntityManager em;

    private User saveUser() {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private CommunityProfile saveProfile(User user, BeltRank beltRank) {
        return em.persist(CommunityProfile.builder()
                .user(user)
                .beltRank(beltRank)
                .build());
    }

    @Test
    @DisplayName("userId 목록으로 커뮤니티 프로필을 한 번에 조회하고, 프로필 없는 유저·목록 밖 유저는 빠진다")
    void findAllByUserIdIn() {
        // given
        User blue = saveUser();
        User purple = saveUser();
        User noProfile = saveUser();
        User notRequested = saveUser();
        saveProfile(blue, BeltRank.BLUE);
        saveProfile(purple, BeltRank.PURPLE);
        saveProfile(notRequested, BeltRank.BLACK);
        em.flush();
        em.clear();

        // when
        List<CommunityProfile> result = communityProfileRepository.findAllByUserIdIn(
                List.of(blue.getId(), purple.getId(), noProfile.getId()));

        // then
        assertThat(result)
                .extracting(profile -> profile.getUser().getId(), CommunityProfile::getBeltRank)
                .containsExactlyInAnyOrder(
                        org.assertj.core.groups.Tuple.tuple(blue.getId(), BeltRank.BLUE),
                        org.assertj.core.groups.Tuple.tuple(purple.getId(), BeltRank.PURPLE)
                );
    }
}
