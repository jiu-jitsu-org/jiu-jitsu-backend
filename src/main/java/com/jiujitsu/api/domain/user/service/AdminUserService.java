package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.admin.entity.Account;
import com.jiujitsu.api.domain.admin.service.AdminAuthenticationFacade;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.entity.CompetitionRecord;
import com.jiujitsu.api.domain.community.profile.repository.CommunityProfileRepository;
import com.jiujitsu.api.domain.competition.entity.CompetitionInterest;
import com.jiujitsu.api.domain.competition.repository.CompetitionInterestRepository;
import com.jiujitsu.api.domain.user.dto.*;
import com.jiujitsu.api.domain.user.entity.*;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.repository.UserSanctionRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminUserService {

    private final UserRepository userRepository;
    private final UserSanctionRepository sanctionRepository;
    private final CompetitionInterestRepository interestRepository;
    private final CommunityProfileRepository profileRepository;
    private final AdminAuthenticationFacade adminAuthFacade;

    // ── 목록 조회 ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AdminUserListItem> getUsers(AdminUserListRequest req, int page, int size) {
        Sort sort = switch (req.sort() != null ? req.sort() : "createdAt") {
            case "reportCount"    -> Sort.by(Sort.Direction.DESC, "reportCount");
            case "violationCount" -> Sort.by(Sort.Direction.DESC, "violationCount");
            default               -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<User> spec = buildSpec(req);
        return userRepository.findAll(spec, pageable)
                .map(user -> {
                    UserSanction active = getActiveSanction(user.getId());
                    return toListItem(user, active);
                });
    }

    private Specification<User> buildSpec(AdminUserListRequest req) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (StringUtils.hasText(req.keyword())) {
                String pattern = "%" + req.keyword().trim() + "%";
                predicates.add(cb.or(
                        cb.like(root.get("nickname"), pattern),
                        cb.like(root.get("email"), pattern)
                ));
            }
            if (req.status() != null) {
                predicates.add(cb.equal(root.get("status"), req.status()));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    // ── 상세 조회 ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public AdminUserDetailResponse getUser(Long userId) {
        User user = findUser(userId);
        UserSanction active = getActiveSanction(userId);

        List<CompetitionInterest> interests = interestRepository.findByUserIdWithCompetition(userId);
        List<AdminUserDetailResponse.InterestItem> interestItems = interests.stream()
                .map(i -> new AdminUserDetailResponse.InterestItem(
                        i.getCompetition().getId(),
                        i.getCompetition().getName(),
                        i.getCompetition().getOpenDate(),
                        i.isNoticeEnabled()))
                .toList();

        List<AdminUserDetailResponse.CompetitionRecordItem> recordItems = profileRepository.findByUser(user)
                .map(CommunityProfile::getCompetitions)
                .orElse(List.of())
                .stream()
                .map(r -> new AdminUserDetailResponse.CompetitionRecordItem(
                        r.getId(),
                        r.getCompetition() != null ? r.getCompetition().getId() : null,
                        r.getCompetitionName(),
                        r.getCompetitionYear(),
                        r.isCompetitionPinned()))
                .toList();

        List<AdminUserDetailResponse.SanctionHistoryItem> history = sanctionRepository
                .findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toSanctionHistoryItem)
                .toList();

        return new AdminUserDetailResponse(
                user.getId(), user.getNickname(), user.getEmail(),
                user.getSnsProvider(), user.getAccountType(), user.getRole(),
                user.getStatus(), user.getCreatedAt(), user.getDeletedAt(),
                user.getReportCount(), user.getViolationCount(),
                active != null ? active.getType() : null,
                active != null ? active.getEndedAt() : null,
                interestItems, recordItems, history
        );
    }

    // ── 제재 처리 ──────────────────────────────────────────────────

    public void applySanction(Long userId, AdminSanctionRequest req) {
        User user = findUser(userId);
        Account operator = adminAuthFacade.getCurrentAccount();

        if (req.type() == SanctionType.SUSPENSION && req.durationDays() == null) {
            throw new ErrorException(ErrorCode.SANCTION_DURATION_REQUIRED);
        }

        LocalDateTime startedAt = LocalDateTime.now();
        LocalDateTime endedAt = req.type() == SanctionType.SUSPENSION
                ? startedAt.plusDays(req.durationDays())
                : null;

        UserSanction sanction = new UserSanction(
                user, req.type(), req.reason(), req.reasonDetail(),
                startedAt, endedAt, operator);
        sanctionRepository.save(sanction);

        // 일시정지·영구차단은 계정 정지
        if (req.type() == SanctionType.SUSPENSION || req.type() == SanctionType.PERMANENT_BAN) {
            user.suspend();
        }
    }

    // ── 제재 해제 ──────────────────────────────────────────────────

    public void releaseSanction(Long userId, Long sanctionId, AdminSanctionReleaseRequest req) {
        UserSanction sanction = sanctionRepository.findByIdWithDetails(sanctionId)
                .orElseThrow(() -> new ErrorException(ErrorCode.SANCTION_NOT_FOUND));

        if (!sanction.getUser().getId().equals(userId)) {
            throw new ErrorException(ErrorCode.SANCTION_NOT_FOUND);
        }
        if (!sanction.isActive()) {
            throw new ErrorException(ErrorCode.SANCTION_ALREADY_RELEASED);
        }

        Account operator = adminAuthFacade.getCurrentAccount();
        sanction.release(operator, req.reason());

        // 더 이상 활성 제재가 없으면 계정 복구
        boolean hasOtherActive = sanctionRepository.findActiveSanctions(userId).stream()
                .anyMatch(s -> !s.getId().equals(sanctionId));
        if (!hasOtherActive) {
            User user = findUser(userId);
            user.restore();
        }
    }

    // ── 탈퇴 목록 ──────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Page<AdminWithdrawalListItem> getWithdrawals(int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return userRepository.findWithdrawals(pageable).map(this::toWithdrawalItem);
    }

    // ── 운영자 대신 탈퇴 처리 ─────────────────────────────────────

    public void withdrawOnBehalf(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        if (user.getDeletedAt() != null) {
            throw new ErrorException(ErrorCode.USER_ALREADY_DEACTIVATED);
        }

        user.getAppInfos().clear();
        user.updateStatus(UserStatus.DELETED);
    }

    // ── 유틸 ──────────────────────────────────────────────────────

    private User findUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));
    }

    private UserSanction getActiveSanction(Long userId) {
        List<UserSanction> active = sanctionRepository.findActiveSanctions(userId);
        return active.isEmpty() ? null : active.get(0);
    }

    private AdminUserListItem toListItem(User user, UserSanction active) {
        return new AdminUserListItem(
                user.getId(), user.getNickname(), user.getEmail(),
                user.getSnsProvider(), user.getAccountType(), user.getRole(),
                user.getStatus(), user.getCreatedAt(), user.getDeletedAt(),
                user.getReportCount(), user.getViolationCount(),
                active != null ? active.getType() : null,
                active != null ? active.getEndedAt() : null
        );
    }

    private AdminUserDetailResponse.SanctionHistoryItem toSanctionHistoryItem(UserSanction s) {
        return new AdminUserDetailResponse.SanctionHistoryItem(
                s.getId(), s.getType(), s.getReason(), s.getReasonDetail(),
                s.getStartedAt(), s.getEndedAt(),
                s.getProcessedBy() != null ? s.getProcessedBy().getName() : null,
                s.getReleasedAt(),
                s.getReleasedBy() != null ? s.getReleasedBy().getName() : null,
                s.getReleasedReason()
        );
    }

    private AdminWithdrawalListItem toWithdrawalItem(User user) {
        LocalDateTime requestedAt = user.getDeletedAt();
        LocalDateTime scheduledAt = requestedAt != null ? requestedAt.plusDays(30) : null;
        AdminWithdrawalListItem.WithdrawalStatus status =
                scheduledAt != null && LocalDateTime.now().isAfter(scheduledAt)
                        ? AdminWithdrawalListItem.WithdrawalStatus.DELETED
                        : AdminWithdrawalListItem.WithdrawalStatus.PENDING;
        return new AdminWithdrawalListItem(
                user.getId(), user.getNickname(), requestedAt, scheduledAt, status);
    }
}
