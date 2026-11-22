package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.DeadlineNotification;
import com.jiujitsu.api.domain.competition.entity.DeadlineNotificationStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface DeadlineNotificationRepository extends JpaRepository<DeadlineNotification, Long> {

    List<DeadlineNotification> findAllByStatus(DeadlineNotificationStatus status);

    // 오늘 마감 알림 대상 대회 수
    long countByDeadlineDateAndStatusIn(LocalDate deadlineDate, List<DeadlineNotificationStatus> statuses);

    List<DeadlineNotification> findAllByDeadlineDate(LocalDate deadlineDate);
}
