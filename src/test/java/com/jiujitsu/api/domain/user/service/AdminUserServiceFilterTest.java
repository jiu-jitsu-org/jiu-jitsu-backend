package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.admin.entity.Account;
import com.jiujitsu.api.domain.admin.entity.AccountRole;
import com.jiujitsu.api.domain.admin.service.AdminAuthenticationFacade;
import com.jiujitsu.api.domain.user.dto.AdminUserListItem;
import com.jiujitsu.api.domain.user.dto.AdminUserListRequest;
import com.jiujitsu.api.domain.user.entity.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.dao.InvalidDataAccessApiUsageException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// sanctionType 필터 서브쿼리는 실제 쿼리 실행 결과로 검증해야 한다
@DataJpaTest
@Import(AdminUserService.class)
class AdminUserServiceFilterTest {

    @Autowired AdminUserService service;
    @Autowired TestEntityManager em;

    @MockBean AdminAuthenticationFacade adminAuthFacade;

    private Account operator;

    @BeforeEach
    void setUp() {
        operator = em.persist(Account.builder()
                .email("admin@test.com")
                .passwordHash("hash")
                .name("운영자")
                .role(AccountRole.OPERATOR)
                .build());
    }

    private User saveUser(String snsId) {
        return em.persist(User.builder()
                .snsProvider(SnsProvider.KAKAO)
                .snsId(snsId)
                .nickname(snsId)
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build());
    }

    private void saveActiveSanction(User user, SanctionType type) {
        LocalDateTime start = LocalDateTime.now().minusDays(1);
        LocalDateTime end = type == SanctionType.SUSPENSION ? start.plusDays(7) : null;
        em.persistAndFlush(new UserSanction(user, type, SanctionReason.PROFANITY, null, start, end, operator));
    }

    private void saveExpiredSuspension(User user) {
        em.persistAndFlush(new UserSanction(user, SanctionType.SUSPENSION, SanctionReason.HATE, null,
                LocalDateTime.now().minusDays(10), LocalDateTime.now().minusDays(3), operator));
    }

    private void saveReleasedWarning(User user) {
        UserSanction s = em.persistAndFlush(new UserSanction(user, SanctionType.WARNING,
                SanctionReason.ADVERTISING, null, LocalDateTime.now().minusDays(5), null, operator));
        s.release(operator, "해제");
        em.flush();
    }

    private Long[] ids(Page<AdminUserListItem> page) {
        return page.getContent().stream().map(AdminUserListItem::id).toArray(Long[]::new);
    }

    @Test
    @DisplayName("sanctionType=NONE — 활성 제재 없는 유저만 반환 (만료·해제는 포함)")
    void filter_none() {
        User clean = saveUser("user-clean");
        User warned = saveUser("user-warned");
        User suspended = saveUser("user-suspended");
        User expired = saveUser("user-expired");     // 만료된 제재 → 활성 제재 없음
        User released = saveUser("user-released");   // 해제된 제재 → 활성 제재 없음

        saveActiveSanction(warned, SanctionType.WARNING);
        saveActiveSanction(suspended, SanctionType.SUSPENSION);
        saveExpiredSuspension(expired);
        saveReleasedWarning(released);
        em.clear();

        Page<AdminUserListItem> result = service.getUsers(
                new AdminUserListRequest(null, null, "NONE", null), 0, 20);

        assertThat(ids(result)).containsExactlyInAnyOrder(clean.getId(), expired.getId(), released.getId());
    }

    @Test
    @DisplayName("sanctionType=WARNING — 활성 경고가 있는 유저만 반환")
    void filter_warning() {
        User warned = saveUser("user-w");
        User suspended = saveUser("user-s");
        User clean = saveUser("user-c");

        saveActiveSanction(warned, SanctionType.WARNING);
        saveActiveSanction(suspended, SanctionType.SUSPENSION);
        em.clear();

        Page<AdminUserListItem> result = service.getUsers(
                new AdminUserListRequest(null, null, "WARNING", null), 0, 20);

        assertThat(ids(result)).containsExactly(warned.getId());
    }

    @Test
    @DisplayName("sanctionType=SUSPENSION — 활성 일시정지가 있는 유저만 반환, 만료된 건 제외")
    void filter_suspension() {
        User active = saveUser("user-active-sus");
        User expired = saveUser("user-expired-sus");

        saveActiveSanction(active, SanctionType.SUSPENSION);
        saveExpiredSuspension(expired);
        em.clear();

        Page<AdminUserListItem> result = service.getUsers(
                new AdminUserListRequest(null, null, "SUSPENSION", null), 0, 20);

        assertThat(ids(result)).containsExactly(active.getId());
    }

    @Test
    @DisplayName("sanctionType=PERMANENT_BAN — 영구차단 유저만 반환")
    void filter_permanentBan() {
        User banned = saveUser("user-banned");
        User warned = saveUser("user-warned2");

        saveActiveSanction(banned, SanctionType.PERMANENT_BAN);
        saveActiveSanction(warned, SanctionType.WARNING);
        em.clear();

        Page<AdminUserListItem> result = service.getUsers(
                new AdminUserListRequest(null, null, "PERMANENT_BAN", null), 0, 20);

        assertThat(ids(result)).containsExactly(banned.getId());
    }

    @Test
    @DisplayName("sanctionType에 잘못된 값 — Spring이 InvalidDataAccessApiUsageException으로 래핑")
    void filter_invalidSanctionType() {
        assertThatThrownBy(() -> service.getUsers(
                new AdminUserListRequest(null, null, "INVALID", null), 0, 20))
                .isInstanceOf(InvalidDataAccessApiUsageException.class)
                .hasMessageContaining("INVALID");
    }
}
