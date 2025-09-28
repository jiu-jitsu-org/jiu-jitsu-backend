package com.jiujitsu.api.domain.user.service;

import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserCleanupService {

    private final UserRepository userRepository;

    @Scheduled(cron = "0 0 0 * * ?") // Runs every day at midnight
    @Transactional
    public void cleanupExpiredDeletedUsers() {
        log.info("----- 회원탈퇴 유저 정보 삭제 start -----");
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        
        List<User> expiredUsers = userRepository.findByStatusAndDeletedAtBefore(
            UserStatus.DELETED, 
            thirtyDaysAgo
        );
        
        if (!expiredUsers.isEmpty()) {
            log.info("Deleting {} users whose grace period has expired", expiredUsers.size());
            userRepository.deleteAll(expiredUsers);
            log.info("----- 회원탈퇴 유저 정보 삭제 end -----");
        } else {
            log.info("----- 회원탈퇴 유저 없음 end -----");
        }
    }
}
