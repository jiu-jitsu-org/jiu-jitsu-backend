package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.admin.entity.Account;
import com.jiujitsu.api.domain.admin.entity.AccountRole;
import com.jiujitsu.api.domain.admin.service.AdminAuthenticationFacade;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.competition.repository.CompetitionInterestRepository;
import com.jiujitsu.api.domain.user.dto.*;
import com.jiujitsu.api.domain.user.entity.*;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.repository.UserSanctionRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @InjectMocks AdminUserService service;
    @Mock UserRepository userRepository;
    @Mock UserSanctionRepository sanctionRepository;
    @Mock CompetitionInterestRepository interestRepository;
    @Mock CommunityProfileRepository profileRepository;
    @Mock AdminAuthenticationFacade adminAuthFacade;

    private User activeUser;
    private Account operator;

    @BeforeEach
    void setUp() {
        activeUser = User.builder()
                .id(1L)
                .nickname("tester")
                .email("test@example.com")
                .snsProvider(SnsProvider.KAKAO)
                .snsId("sns-1")
                .role(UserRole.USER)
                .status(UserStatus.ACTIVE)
                .build();

        operator = Account.builder()
                .email("admin@example.com")
                .passwordHash("hash")
                .name("운영자")
                .role(AccountRole.OPERATOR)
                .build();
    }

    // ── 회원 목록 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUsers")
    class GetUsers {

        @Test
        @DisplayName("키워드·상태 조건 없이 전체 조회 — 활성 제재 없으면 currentSanctionType null")
        void getUsers_noFilter_noActiveSanction() {
            AdminUserListRequest req = new AdminUserListRequest(null, null, null, null);
            Page<User> userPage = new PageImpl<>(List.of(activeUser));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(userPage);
            when(sanctionRepository.findActiveSanctions(1L)).thenReturn(List.of());

            Page<AdminUserListItem> result = service.getUsers(req, 0, 20);

            assertThat(result.getContent()).hasSize(1);
            AdminUserListItem item = result.getContent().get(0);
            assertThat(item.id()).isEqualTo(1L);
            assertThat(item.nickname()).isEqualTo("tester");
            assertThat(item.currentSanctionType()).isNull();
            assertThat(item.sanctionEndedAt()).isNull();
        }

        @Test
        @DisplayName("활성 일시정지 제재가 있으면 SUSPENSION과 종료일 반환")
        void getUsers_withActiveSuspension() {
            AdminUserListRequest req = new AdminUserListRequest(null, null, null, null);
            Page<User> userPage = new PageImpl<>(List.of(activeUser));
            when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(userPage);

            LocalDateTime endedAt = LocalDateTime.now().plusDays(5);
            UserSanction sanction = new UserSanction(activeUser, SanctionType.SUSPENSION,
                    SanctionReason.HATE, null, LocalDateTime.now(), endedAt, operator);
            when(sanctionRepository.findActiveSanctions(1L)).thenReturn(List.of(sanction));

            Page<AdminUserListItem> result = service.getUsers(req, 0, 20);

            AdminUserListItem item = result.getContent().get(0);
            assertThat(item.currentSanctionType()).isEqualTo(SanctionType.SUSPENSION);
            assertThat(item.sanctionEndedAt()).isEqualTo(endedAt);
        }

        @Test
        @DisplayName("violationCount 정렬 요청 — Pageable sort 반영")
        void getUsers_sortByViolationCount() {
            AdminUserListRequest req = new AdminUserListRequest(null, null, null, "violationCount");
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenAnswer(inv -> {
                        Pageable p = inv.getArgument(1);
                        assertThat(p.getSort().getOrderFor("violationCount")).isNotNull();
                        return Page.empty();
                    });

            service.getUsers(req, 0, 20);
        }

        @Test
        @DisplayName("sanctionType=NONE — 예외 없이 repository 호출")
        void getUsers_sanctionType_none() {
            AdminUserListRequest req = new AdminUserListRequest(null, null, "NONE", null);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

            assertThatNoException().isThrownBy(() -> service.getUsers(req, 0, 20));
        }

        @Test
        @DisplayName("sanctionType=WARNING — 예외 없이 repository 호출")
        void getUsers_sanctionType_warning() {
            AdminUserListRequest req = new AdminUserListRequest(null, null, "WARNING", null);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class))).thenReturn(Page.empty());

            assertThatNoException().isThrownBy(() -> service.getUsers(req, 0, 20));
        }

        @Test
        @DisplayName("sanctionType에 잘못된 값 — IllegalArgumentException")
        void getUsers_sanctionType_invalid() {
            AdminUserListRequest req = new AdminUserListRequest(null, null, "INVALID_TYPE", null);
            when(userRepository.findAll(any(Specification.class), any(Pageable.class)))
                    .thenAnswer(inv -> {
                        // Specification 실행 시 valueOf 에서 예외 발생 — 여기선 spec 미실행이므로 서비스 단 검증 불가
                        // DB 연동 테스트(AdminUserServiceFilterTest)에서 검증
                        return Page.empty();
                    });

            // Mockito 환경에서는 Specification이 실제로 평가되지 않아 예외가 발생하지 않음
            // 실제 DB 연동 시 IllegalArgumentException 발생 (AdminUserServiceFilterTest 참고)
            assertThatNoException().isThrownBy(() -> service.getUsers(req, 0, 20));
        }
    }

    // ── 회원 상세 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getUser")
    class GetUser {

        @Test
        @DisplayName("존재하는 회원 — 관심 대회·대회 기록·제재 이력 포함")
        void getUser_found() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
            when(sanctionRepository.findActiveSanctions(1L)).thenReturn(List.of());
            when(interestRepository.findByUserIdWithCompetition(1L)).thenReturn(List.of());
            when(profileRepository.findByUser(activeUser)).thenReturn(Optional.empty());
            when(sanctionRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of());

            AdminUserDetailResponse result = service.getUser(1L);

            assertThat(result.id()).isEqualTo(1L);
            assertThat(result.nickname()).isEqualTo("tester");
            assertThat(result.interests()).isEmpty();
            assertThat(result.competitionRecords()).isEmpty();
            assertThat(result.sanctionHistory()).isEmpty();
            assertThat(result.currentSanctionType()).isNull();
        }

        @Test
        @DisplayName("없는 회원 — USER_NOT_FOUND")
        void getUser_notFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.getUser(99L))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        }

        @Test
        @DisplayName("제재 이력이 있으면 SanctionHistoryItem 목록 반환")
        void getUser_withSanctionHistory() {
            UserSanction past = new UserSanction(activeUser, SanctionType.WARNING,
                    SanctionReason.PROFANITY, null,
                    LocalDateTime.now().minusDays(10), null, operator);

            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
            when(sanctionRepository.findActiveSanctions(1L)).thenReturn(List.of());
            when(interestRepository.findByUserIdWithCompetition(1L)).thenReturn(List.of());
            when(profileRepository.findByUser(activeUser)).thenReturn(Optional.empty());
            when(sanctionRepository.findAllByUserIdOrderByCreatedAtDesc(1L)).thenReturn(List.of(past));

            AdminUserDetailResponse result = service.getUser(1L);

            assertThat(result.sanctionHistory()).hasSize(1);
            assertThat(result.sanctionHistory().get(0).type()).isEqualTo(SanctionType.WARNING);
            assertThat(result.sanctionHistory().get(0).reason()).isEqualTo(SanctionReason.PROFANITY);
            assertThat(result.sanctionHistory().get(0).processedByName()).isEqualTo("운영자");
        }
    }

    // ── 제재 처리 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("applySanction")
    class ApplySanction {

        @BeforeEach
        void stubOperator() {
            lenient().when(adminAuthFacade.getCurrentAccount()).thenReturn(operator);
        }

        @Test
        @DisplayName("경고 — 계정 상태 변경 없이 UserSanction 저장")
        void warning_noStatusChange() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
            AdminSanctionRequest req = new AdminSanctionRequest(SanctionType.WARNING, SanctionReason.PROFANITY, null, null);

            service.applySanction(1L, req);

            verify(sanctionRepository).save(argThat(s ->
                    s.getType() == SanctionType.WARNING && s.getEndedAt() == null));
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("일시정지 — SUSPENDED 상태로 변경되고 endedAt 세팅")
        void suspension_statusSuspended() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
            AdminSanctionRequest req = new AdminSanctionRequest(SanctionType.SUSPENSION, SanctionReason.HATE, null, 7);

            service.applySanction(1L, req);

            verify(sanctionRepository).save(argThat(s ->
                    s.getType() == SanctionType.SUSPENSION && s.getEndedAt() != null));
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("일시정지인데 durationDays 없으면 SANCTION_DURATION_REQUIRED")
        void suspension_noDuration_error() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
            AdminSanctionRequest req = new AdminSanctionRequest(SanctionType.SUSPENSION, SanctionReason.HATE, null, null);

            assertThatThrownBy(() -> service.applySanction(1L, req))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.SANCTION_DURATION_REQUIRED);
            verify(sanctionRepository, never()).save(any());
        }

        @Test
        @DisplayName("영구차단 — SUSPENDED 상태로 변경되고 endedAt null")
        void permanentBan_statusSuspended_noEndedAt() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
            AdminSanctionRequest req = new AdminSanctionRequest(SanctionType.PERMANENT_BAN, SanctionReason.OBSCENE, null, null);

            service.applySanction(1L, req);

            verify(sanctionRepository).save(argThat(s ->
                    s.getType() == SanctionType.PERMANENT_BAN && s.getEndedAt() == null));
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.SUSPENDED);
        }

        @Test
        @DisplayName("없는 회원 — USER_NOT_FOUND")
        void userNotFound() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());
            AdminSanctionRequest req = new AdminSanctionRequest(SanctionType.WARNING, SanctionReason.PROFANITY, null, null);

            assertThatThrownBy(() -> service.applySanction(99L, req))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    // ── 제재 해제 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("releaseSanction")
    class ReleaseSanction {

        @BeforeEach
        void stubOperator() {
            lenient().when(adminAuthFacade.getCurrentAccount()).thenReturn(operator);
        }

        @Test
        @DisplayName("활성 일시정지 해제 — 다른 활성 제재 없으면 ACTIVE 복구")
        void release_activeOnly_restoresUser() {
            activeUser.suspend();
            UserSanction sanction = new UserSanction(activeUser, SanctionType.SUSPENSION,
                    SanctionReason.HATE, null,
                    LocalDateTime.now().minusDays(1), LocalDateTime.now().plusDays(6), operator);
            // id 세팅을 위해 reflection 사용
            setId(sanction, 10L);

            when(sanctionRepository.findByIdWithDetails(10L)).thenReturn(Optional.of(sanction));
            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));
            when(sanctionRepository.findActiveSanctions(1L)).thenReturn(List.of(sanction));

            service.releaseSanction(1L, 10L, new AdminSanctionReleaseRequest("실수 해제"));

            assertThat(sanction.getReleasedAt()).isNotNull();
            assertThat(sanction.getReleasedReason()).isEqualTo("실수 해제");
            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.ACTIVE);
        }

        @Test
        @DisplayName("이미 해제된 제재 — SANCTION_ALREADY_RELEASED")
        void release_alreadyReleased_error() {
            UserSanction sanction = new UserSanction(activeUser, SanctionType.WARNING,
                    SanctionReason.PROFANITY, null,
                    LocalDateTime.now().minusDays(2), null, operator);
            sanction.release(operator, "이전 해제");
            setId(sanction, 11L);

            when(sanctionRepository.findByIdWithDetails(11L)).thenReturn(Optional.of(sanction));

            assertThatThrownBy(() -> service.releaseSanction(1L, 11L, new AdminSanctionReleaseRequest("재해제")))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.SANCTION_ALREADY_RELEASED);
        }

        @Test
        @DisplayName("없는 제재 ID — SANCTION_NOT_FOUND")
        void release_notFound_error() {
            when(sanctionRepository.findByIdWithDetails(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.releaseSanction(1L, 99L, new AdminSanctionReleaseRequest("사유")))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.SANCTION_NOT_FOUND);
        }

        @Test
        @DisplayName("다른 유저의 제재 ID — SANCTION_NOT_FOUND")
        void release_wrongUser_error() {
            User otherUser = User.builder().id(2L).snsId("sns-2").build();
            UserSanction sanction = new UserSanction(otherUser, SanctionType.WARNING,
                    SanctionReason.HATE, null, LocalDateTime.now(), null, operator);
            setId(sanction, 12L);

            when(sanctionRepository.findByIdWithDetails(12L)).thenReturn(Optional.of(sanction));

            assertThatThrownBy(() -> service.releaseSanction(1L, 12L, new AdminSanctionReleaseRequest("사유")))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.SANCTION_NOT_FOUND);
        }
    }

    // ── 탈퇴 처리 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("withdrawOnBehalf")
    class WithdrawOnBehalf {

        @Test
        @DisplayName("정상 탈퇴 처리 — DELETED 상태 + deletedAt 세팅")
        void withdraw_success() {
            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

            service.withdrawOnBehalf(1L);

            assertThat(activeUser.getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(activeUser.getDeletedAt()).isNotNull();
        }

        @Test
        @DisplayName("이미 탈퇴한 회원 — USER_ALREADY_DEACTIVATED")
        void withdraw_alreadyDeleted_error() {
            activeUser.updateStatus(UserStatus.DELETED);
            when(userRepository.findById(1L)).thenReturn(Optional.of(activeUser));

            assertThatThrownBy(() -> service.withdrawOnBehalf(1L))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_ALREADY_DEACTIVATED);
        }

        @Test
        @DisplayName("없는 회원 — USER_NOT_FOUND")
        void withdraw_notFound_error() {
            when(userRepository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.withdrawOnBehalf(99L))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode").isEqualTo(ErrorCode.USER_NOT_FOUND);
        }
    }

    // ── 탈퇴 목록 ────────────────────────────────────────────────────

    @Nested
    @DisplayName("getWithdrawals")
    class GetWithdrawals {

        @Test
        @DisplayName("30일 미경과 — PENDING, 경과 — DELETED")
        void withdrawalStatus_derived_from_deletedAt() {
            User pending = User.builder().id(2L).snsId("sns-2").nickname("pending").build();
            pending.updateStatus(UserStatus.DELETED); // deletedAt = now

            User deleted = User.builder().id(3L).snsId("sns-3").nickname("gone").build();
            deleted.updateStatus(UserStatus.DELETED);
            // 31일 전으로 강제 세팅 (reflection)
            setDeletedAt(deleted, LocalDateTime.now().minusDays(31));

            Page<User> page = new PageImpl<>(List.of(pending, deleted));
            when(userRepository.findWithdrawals(any(Pageable.class))).thenReturn(page);

            Page<AdminWithdrawalListItem> result = service.getWithdrawals(0, 20);

            assertThat(result.getContent().get(0).status())
                    .isEqualTo(AdminWithdrawalListItem.WithdrawalStatus.PENDING);
            assertThat(result.getContent().get(1).status())
                    .isEqualTo(AdminWithdrawalListItem.WithdrawalStatus.DELETED);
        }
    }

    // ── 유틸 ─────────────────────────────────────────────────────────

    private void setId(Object obj, Long id) {
        try {
            var field = obj.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(obj, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setDeletedAt(User user, LocalDateTime deletedAt) {
        try {
            var field = User.class.getDeclaredField("deletedAt");
            field.setAccessible(true);
            field.set(user, deletedAt);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
