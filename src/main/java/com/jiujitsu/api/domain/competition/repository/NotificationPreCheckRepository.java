package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.NotificationPreCheck;
import com.jiujitsu.api.domain.competition.entity.NotificationPreCheckStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface NotificationPreCheckRepository extends JpaRepository<NotificationPreCheck, Long> {

    List<NotificationPreCheck> findAllByStatus(NotificationPreCheckStatus status);

    // 오늘 발송 대상 중 아직 확인 전인 항목 수
    long countByStatusAndDeadlineDate(NotificationPreCheckStatus status, LocalDate deadlineDate);
}
