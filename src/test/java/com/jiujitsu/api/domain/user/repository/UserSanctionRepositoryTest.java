package com.jiujitsu.api.domain.user.repository;

import com.jiujitsu.api.domain.admin.entity.Account;
import com.jiujitsu.api.domain.admin.entity.AccountRole;
import com.jiujitsu.api.domain.admin.repository.AccountRepository;
import com.jiujitsu.api.domain.user.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserSanctionRepositoryTest {

    @Autowired UserSanctionRepository sanctionRepository;
    @Autowired UserRepository userRepository;
    @Autowired AccountRepository accountRepository;
    @Autowired TestEntityManager em;

    private User user;
    private Account operator;

    @BeforeEach
    void setUp() {
        user = userRepository.save(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(UUID.randomUUID().toString())
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());

        operator = accountRepository.save(Account.builder()
                .email("admin@test.com")
                .passwordHash("hash")
                .name("운영자")
                .role(AccountRole.OPERATOR)
                .build());
    }

    @Test
    @DisplayName("활성 제재만 반환 — 해제된 것, 기간 만료된 것은 제외")
    void findActiveSanctions() {
        // 활성 경고
        UserSanction active = sanctionRepository.save(new UserSanction(
                user, SanctionType.WARNING, SanctionReason.PROFANITY, null,
                LocalDateTime.now(), null, operator));

        // 해제된 일시정지
        UserSanction released = sanctionRepository.save(new UserSanction(
                user, SanctionType.SUSPENSION, SanctionReason.HATE, null,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().plusDays(4),
                operator));
        released.release(operator, "해제");

        // 기간 만료된 일시정지
        sanctionRepository.save(new UserSanction(
                user, SanctionType.SUSPENSION, SanctionReason.ADVERTISING, null,
                LocalDateTime.now().minusDays(10),
                LocalDateTime.now().minusDays(3),  // 이미 종료
                operator));

        em.flush();
        em.clear();

        List<UserSanction> result = sanctionRepository.findActiveSanctions(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(active.getId());
        assertThat(result.get(0).getType()).isEqualTo(SanctionType.WARNING);
    }

    @Test
    @DisplayName("영구차단은 endedAt이 null — 활성으로 계속 조회됨")
    void findActiveSanctions_permanentBan_alwaysActive() {
        sanctionRepository.save(new UserSanction(
                user, SanctionType.PERMANENT_BAN, SanctionReason.OBSCENE, null,
                LocalDateTime.now(), null, operator));

        em.flush();
        em.clear();

        List<UserSanction> result = sanctionRepository.findActiveSanctions(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getType()).isEqualTo(SanctionType.PERMANENT_BAN);
    }

    @Test
    @DisplayName("findByIdWithDetails — processedBy, releasedBy 페치 조인")
    void findByIdWithDetails_fetchesRelations() {
        UserSanction sanction = sanctionRepository.save(new UserSanction(
                user, SanctionType.WARNING, SanctionReason.PROFANITY, null,
                LocalDateTime.now(), null, operator));
        sanction.release(operator, "해제 사유");

        em.flush();
        em.clear();

        var result = sanctionRepository.findByIdWithDetails(sanction.getId());

        assertThat(result).isPresent();
        assertThat(result.get().getProcessedBy().getName()).isEqualTo("운영자");
        assertThat(result.get().getReleasedBy().getName()).isEqualTo("운영자");
        assertThat(result.get().getReleasedReason()).isEqualTo("해제 사유");
    }

    @Test
    @DisplayName("findAllByUserIdOrderByCreatedAtDesc — 최신순 반환")
    void findAllByUserIdOrderByCreatedAtDesc() {
        UserSanction s1 = sanctionRepository.save(new UserSanction(
                user, SanctionType.WARNING, SanctionReason.PROFANITY, null,
                LocalDateTime.now().minusDays(2), null, operator));
        UserSanction s2 = sanctionRepository.save(new UserSanction(
                user, SanctionType.PERMANENT_BAN, SanctionReason.OBSCENE, null,
                LocalDateTime.now(), null, operator));

        em.flush();
        em.clear();

        List<UserSanction> result = sanctionRepository.findAllByUserIdOrderByCreatedAtDesc(user.getId());

        assertThat(result).hasSize(2);
        assertThat(result.get(0).getId()).isEqualTo(s2.getId()); // 최신이 먼저
    }
}
