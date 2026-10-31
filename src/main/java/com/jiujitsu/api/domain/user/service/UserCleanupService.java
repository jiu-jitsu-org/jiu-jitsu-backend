package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final UserRepository userRepository;

    // 트랜잭션 분리를 위한 self-injection (@Lazy 로 순환 참조 방지)
    @Lazy
    @Autowired
    private UserCleanupService self;

    @Scheduled(cron = "0 0 0 * * ?") // Runs every day at midnight
    public void cleanupExpiredDeletedUsers() {
        log.info("----- 회원탈퇴 유저 정보 삭제 start -----");
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);

        List<User> expiredUsers = userRepository.findByStatusAndDeletedAtBefore(
            UserStatus.DELETED,
            thirtyDaysAgo
        );

        if (expiredUsers.isEmpty()) {
            log.info("----- 회원탈퇴 유저 없음 end -----");
            return;
        }

        log.info("Deleting {} users whose grace period has expired", expiredUsers.size());
        int successCount = 0;
        for (User user : expiredUsers) {
            try {
                self.deleteUser(user);
                successCount++;
            } catch (Exception e) {
                log.error("Failed to delete user id={}: {}", user.getId(), e.getMessage(), e);
            }
        }
        log.info("----- 회원탈퇴 유저 정보 삭제 end (성공: {}/{}) -----", successCount, expiredUsers.size());
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void deleteUser(User user) {
        userRepository.delete(user);
    }
}
