package com.jiujitsu.api.domain.notice.repository;

import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserNoticeSettingRepository extends JpaRepository<UserNoticeSetting, Long> {

    // 이 대회 마감 알림을 받을 유저: 관심 등록 + 이 대회 알림 켜짐 + 설정의 관심 대회 알림 켜짐
    @Query("""
    SELECT i.user.id
    FROM CompetitionInterest i, UserNoticeSetting s
    WHERE s.user = i.user
      AND i.competition.id = :competitionId
      AND i.noticeEnabled = true
      AND s.competitionEnabled = true
    """)
    List<Long> findCompetitionNoticeRecipientIds(@Param("competitionId") Long competitionId);
}
