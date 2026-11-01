package com.jiujitsu.api.global.log.repository;

import com.jiujitsu.api.global.log.entity.ApiLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface ApiLogRepository extends JpaRepository<ApiLog, Long> {

    Page<ApiLog> findByRequestedAtBetween(LocalDateTime start, LocalDateTime end, Pageable pageable);

    Page<ApiLog> findBySuccess(Boolean success, Pageable pageable);

    Page<ApiLog> findBySuccessAndRequestedAtBetween(Boolean success, LocalDateTime start, LocalDateTime end, Pageable pageable);

    @Modifying
    @Query("DELETE FROM ApiLog a WHERE a.requestedAt < :threshold")
    int deleteByRequestedAtBefore(@Param("threshold") LocalDateTime threshold);
}
