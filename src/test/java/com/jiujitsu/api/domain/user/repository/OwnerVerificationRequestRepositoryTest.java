package com.jiujitsu.api.domain.user.repository;

import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.user.entity.OwnerVerificationRequest;
import com.jiujitsu.api.domain.user.entity.OwnerVerificationStatus;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class OwnerVerificationRequestRepositoryTest {

    @Autowired
    private OwnerVerificationRequestRepository ownerVerificationRequestRepository;

    @Autowired
    private CommunityProfileRepository communityProfileRepository;

    @Autowired
    private TestEntityManager em;

    private User saveUser(UserRole role) {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(role)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private ImageFile saveImage() {
        return em.persist(ImageFile.builder().imageUrl("https://cdn/" + UUID.randomUUID()).build());
    }

    private OwnerVerificationRequest saveRequest(User user) {
        return em.persist(OwnerVerificationRequest.builder()
                .user(user)
                .documentImageFile(saveImage())
                .build());
    }

    @Test
    @DisplayName("상태별로 신청 최신순 페이지를 반환하고 count 도 상태 기준이다")
    void findByStatusWithUser() {
        // given
        User admin = saveUser(UserRole.ADMIN);
        OwnerVerificationRequest first = saveRequest(saveUser(UserRole.USER));
        OwnerVerificationRequest rejected = saveRequest(saveUser(UserRole.USER));
        rejected.reject(admin, null);
        OwnerVerificationRequest second = saveRequest(saveUser(UserRole.USER));
        OwnerVerificationRequest third = saveRequest(saveUser(UserRole.USER));
        em.flush();
        em.clear();

        // when
        Page<OwnerVerificationRequest> page = ownerVerificationRequestRepository.findByStatusWithUser(
                OwnerVerificationStatus.PENDING, PageRequest.of(0, 2));
        Page<OwnerVerificationRequest> rejectedPage = ownerVerificationRequestRepository.findByStatusWithUser(
                OwnerVerificationStatus.REJECTED, PageRequest.of(0, 20));

        // then
        assertThat(page.getTotalElements()).isEqualTo(3);
        assertThat(page.getContent()).extracting(OwnerVerificationRequest::getId)
                .containsExactly(third.getId(), second.getId());
        assertThat(page.getContent().get(0).getUser().getId()).isNotNull();
        assertThat(rejectedPage.getContent()).extracting(OwnerVerificationRequest::getId)
                .containsExactly(rejected.getId());
        assertThat(first.getId()).isNotNull();
    }

    @Test
    @DisplayName("유저의 최신 신청 1건을 반환한다")
    void findLatest() {
        // given
        User admin = saveUser(UserRole.ADMIN);
        User me = saveUser(UserRole.USER);
        OwnerVerificationRequest old = saveRequest(me);
        old.reject(admin, "불명확");
        OwnerVerificationRequest latest = saveRequest(me);
        saveRequest(saveUser(UserRole.USER));
        em.flush();

        // when & then
        assertThat(ownerVerificationRequestRepository.findTopByUserOrderByIdDesc(me))
                .get().extracting(OwnerVerificationRequest::getId).isEqualTo(latest.getId());
        assertThat(ownerVerificationRequestRepository.findTopByUserOrderByIdDesc(saveUser(UserRole.USER))).isEmpty();
    }

    @Test
    @DisplayName("서류 이미지로 신청을 찾는다 (이미지 삭제 시 참조 정리용)")
    void findByDocumentImageFile() {
        OwnerVerificationRequest request = saveRequest(saveUser(UserRole.USER));
        em.flush();

        assertThat(ownerVerificationRequestRepository.findByDocumentImageFile(request.getDocumentImageFile()))
                .extracting(OwnerVerificationRequest::getId).containsExactly(request.getId());
    }

    @Test
    @DisplayName("커뮤니티 프로필이 있는 유저 id 만 반환한다")
    void findUserIdsWithCommunityProfile() {
        User withProfile = saveUser(UserRole.USER);
        User withoutProfile = saveUser(UserRole.USER);
        em.persist(CommunityProfile.builder().user(withProfile).build());
        em.flush();

        List<Long> userIds = communityProfileRepository.findUserIdsByUserIdIn(
                List.of(withProfile.getId(), withoutProfile.getId()));

        assertThat(userIds).containsExactly(withProfile.getId());
    }
}
