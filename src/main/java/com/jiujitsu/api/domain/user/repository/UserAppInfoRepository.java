package com.jiujitsu.api.domain.user.repository;

import com.jiujitsu.api.domain.user.entity.UserAppInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserAppInfoRepository extends JpaRepository<UserAppInfo, Long> {
}
