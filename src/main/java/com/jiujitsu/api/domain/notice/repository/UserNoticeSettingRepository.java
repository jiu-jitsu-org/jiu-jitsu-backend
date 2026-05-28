package com.jiujitsu.api.domain.notice.repository;

import com.jiujitsu.api.domain.notice.entity.UserNoticeSetting;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserNoticeSettingRepository extends JpaRepository<UserNoticeSetting, Long> {
}
