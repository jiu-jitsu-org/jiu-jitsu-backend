package com.jiujitsu.api.domain.notice.repository;

import com.jiujitsu.api.domain.notice.entity.CompetitionDeadlineNoticeLog;
import com.jiujitsu.api.domain.notice.entity.CompetitionDeadlineType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Set;

public interface CompetitionDeadlineNoticeLogRepository extends JpaRepository<CompetitionDeadlineNoticeLog, Long> {

    // 이 마감 알림을 이미 받은 유저 (유니크 키 앞부분 인덱스를 탄다)
    @Query("""
    SELECT l.userId FROM CompetitionDeadlineNoticeLog l
    WHERE l.competitionId = :competitionId
      AND l.deadlineType = :deadlineType
      AND l.deadline = :deadline
      AND l.userId IN :userIds
    """)
    Set<Long> findNotifiedUserIds(@Param("competitionId") Long competitionId,
                                  @Param("deadlineType") CompetitionDeadlineType deadlineType,
                                  @Param("deadline") LocalDate deadline,
                                  @Param("userIds") Collection<Long> userIds);
}
