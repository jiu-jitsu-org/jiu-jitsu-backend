package com.jiujitsu.api.domain.user.repository;

import com.jiujitsu.api.domain.user.entity.UserAppInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserAppInfoRepository extends JpaRepository<UserAppInfo, Long> {

    /** 현재 유저의 특정 FCM 토큰 행 조회 (로그아웃 시 삭제용) */
    Optional<UserAppInfo> findByTokenAndUserId(String token, Long userId);

    /** 다른 계정에 등록된 같은 FCM 토큰 삭제 (토큰 등록 시 크로스 계정 정리) */
    void deleteByTokenAndUserIdNot(String token, Long userId);

    /** 다른 계정에 등록된 같은 deviceId 삭제 (토큰 등록 시 크로스 계정 정리) */
    void deleteByDeviceIdAndUserIdNot(String deviceId, Long userId);
}
