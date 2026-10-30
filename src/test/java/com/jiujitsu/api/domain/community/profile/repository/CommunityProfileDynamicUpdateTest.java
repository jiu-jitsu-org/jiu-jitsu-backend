package com.jiujitsu.api.domain.community.profile.repository;

import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.OwnerProfile;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class CommunityProfileDynamicUpdateTest {

    @Autowired
    private TestEntityManager em;

    @Test
    @DisplayName("본인 프로필 저장은 바뀐 컬럼만 UPDATE 해, 그사이 관리자 승인이 넣은 관장 프로필 연결을 지우지 않는다")
    void profileSaveKeepsOwnerProfileSetConcurrently() {
        // given: 관장 프로필 없는 커뮤니티 프로필
        User user = em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
        CommunityProfile profile = em.persist(CommunityProfile.builder().user(user).academyName("옛 도장").build());
        OwnerProfile ownerProfile = em.persist(OwnerProfile.builder().build());
        em.flush();
        em.clear();

        // 본인 프로필 저장 트랜잭션이 먼저 읽음 (owner_id = null)
        CommunityProfile loaded = em.find(CommunityProfile.class, profile.getId());

        // 그사이 관리자 승인이 owner_id 를 커밋 (다른 트랜잭션 → 영속성 컨텍스트를 거치지 않는 UPDATE 로 흉내)
        em.getEntityManager()
                .createQuery("UPDATE CommunityProfile c SET c.ownerProfile = :owner WHERE c.id = :id")
                .setParameter("owner", ownerProfile)
                .setParameter("id", profile.getId())
                .executeUpdate();

        // when: 본인 저장이 도장명만 바꿔 flush
        loaded.upsertAcademy("새 도장");
        em.flush();
        em.clear();

        // then
        CommunityProfile result = em.find(CommunityProfile.class, profile.getId());
        assertThat(result.getAcademyName()).isEqualTo("새 도장");
        assertThat(result.getOwnerProfile()).isNotNull();
        assertThat(result.getOwnerProfile().getId()).isEqualTo(ownerProfile.getId());
    }
}
