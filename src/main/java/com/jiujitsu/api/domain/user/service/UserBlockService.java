package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.dto.BlockedUserResponse;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserBlock;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserBlockRepository;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.util.ConstraintViolationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class UserBlockService {

    private final AuthenticationFacade authenticationFacade;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;
    private final TransactionTemplate transactionTemplate;

    /**
     * 유저 차단 (멱등 — 이미 차단했으면 그대로 둔다, #232)
     * 연타·다른 기기·오래 열어 둔 화면에서 다시 눌러도 차단이 풀리지 않는다. 해제는 {@link #unblock} 으로만.
     * 같은 사람의 요청이 동시에 들어와 늦은 쪽이 (차단한 사람, 차단된 사람) 유니크 제약에 걸리면 이미 차단된 것이므로 성공으로 본다.
     * 에러가 난 트랜잭션은 다시 쓸 수 없으므로(PostgreSQL) TransactionTemplate 으로 나눠 실행한다 (좋아요와 같음, #199)
     * @return 항상 true (요청 뒤 차단 상태. 응답 형식은 토글 시절과 같게 둔다)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public boolean block(Long blockedUserId) {
        try {
            transactionTemplate.executeWithoutResult(status -> saveBlockIfAbsent(blockedUserId));
        } catch (DataIntegrityViolationException e) {
            // 이 트랜잭션이 쓰는 유니크는 (blocker, blocked)뿐이다 (PK 는 IDENTITY) → 유니크 위반이면 동시 요청이 먼저 차단한 것.
            // 그 밖의 위반(FK 등)은 그대로 드러낸다
            if (!ConstraintViolationUtil.isUniqueViolation(e)) {
                throw e;
            }
            log.info("차단 동시 요청 충돌 → 이미 차단됨으로 응답 blockedUserId={} cause={}",
                    blockedUserId, e.getMostSpecificCause().getMessage());
        }
        return true;
    }

    private void saveBlockIfAbsent(Long blockedUserId) {
        User blocker = authenticationFacade.getCurrentUser();

        if (blocker.getId().equals(blockedUserId)) {
            throw new ErrorException(ErrorCode.SELF_BLOCK_NOT_ALLOWED);
        }

        // 없는 회원은 404 (USER_NOT_FOUND 는 401 이라 앱이 로그인 만료로 처리한다). 탈퇴(유예 포함) 회원도 공개 프로필처럼 없는 회원
        User blocked = userRepository.findById(blockedUserId)
                .filter(found -> found.getStatus() != UserStatus.DELETED)
                .orElseThrow(() -> new ErrorException(ErrorCode.MEMBER_NOT_FOUND));

        if (userBlockRepository.existsByBlockerIdAndBlockedId(blocker.getId(), blocked.getId())) {
            return;
        }
        userBlockRepository.save(UserBlock.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build());
    }

    /**
     * blocker 가 blocked 를 차단했는지 (한 방향만 — 공개 프로필 isBlocked, #232)
     */
    @Transactional(readOnly = true)
    public boolean hasBlocked(Long blockerId, Long blockedId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    /**
     * 두 사용자 사이에 어느 방향이든 차단 관계가 있는지 확인
     */
    @Transactional(readOnly = true)
    public boolean isBlockedBetween(Long viewerId, Long targetId) {
        return userBlockRepository.existsByBlockerIdAndBlockedId(viewerId, targetId)
                || userBlockRepository.existsByBlockerIdAndBlockedId(targetId, viewerId);
    }

    /**
     * 현재 로그인 유저가 차단한 유저 ID 목록 조회
     */
    @Transactional(readOnly = true)
    public List<Long> getBlockedUserIds() {
        return authenticationFacade.getCurrentUserOptional()
                .map(user -> userBlockRepository.findBlockedIdsByBlockerId(user.getId()))
                .orElse(Collections.emptyList());
    }

    /**
     * 차단한 회원 목록 조회 (최근 차단순, 탈퇴 회원 제외)
     */
    @Transactional(readOnly = true)
    public List<BlockedUserResponse> getBlockedUsers() {
        User blocker = authenticationFacade.getCurrentUser();
        return userBlockRepository.findActiveBlocksByBlockerIdOrderByCreatedAtDesc(blocker.getId())
                .stream()
                .map(BlockedUserResponse::from)
                .toList();
    }

    /**
     * 차단 해제 전용 (이미 해제된 경우 무시 — 다중 기기 안전)
     */
    public void unblock(Long blockedUserId) {
        User blocker = authenticationFacade.getCurrentUser();
        userBlockRepository.findByBlockerIdAndBlockedId(blocker.getId(), blockedUserId)
                .ifPresent(userBlockRepository::delete);
    }
}
