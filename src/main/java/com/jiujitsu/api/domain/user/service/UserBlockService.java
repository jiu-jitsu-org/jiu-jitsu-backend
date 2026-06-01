package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserBlock;
import com.jiujitsu.api.domain.user.repository.UserBlockRepository;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class UserBlockService {

    private final AuthenticationFacade authenticationFacade;
    private final UserRepository userRepository;
    private final UserBlockRepository userBlockRepository;

    /**
     * 유저 차단/차단 해제 (toggle)
     * @return true = 차단, false = 차단 해제
     */
    public boolean toggleBlock(Long blockedUserId) {
        User blocker = authenticationFacade.getCurrentUser();

        if (blocker.getId().equals(blockedUserId)) {
            throw new ErrorException(ErrorCode.SELF_BLOCK_NOT_ALLOWED);
        }

        User blocked = userRepository.findById(blockedUserId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        Optional<UserBlock> existing = userBlockRepository.findByBlockerAndBlocked(blocker, blocked);

        if (existing.isPresent()) {
            userBlockRepository.delete(existing.get());
            return false;
        } else {
            userBlockRepository.save(UserBlock.builder()
                    .blocker(blocker)
                    .blocked(blocked)
                    .build());
            return true;
        }
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
}
