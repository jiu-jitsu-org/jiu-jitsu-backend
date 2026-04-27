package com.jiujitsu.api.global.fcm.repository;

import com.jiujitsu.api.global.fcm.entity.PushSendLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PushSendLogRepository extends JpaRepository<PushSendLog, Long> {
}
