package com.jiujitsu.api.domain.user.repository;

import com.jiujitsu.api.domain.user.entity.UserSanction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSanctionRepository extends JpaRepository<UserSanction, Long> {

    List<UserSanction> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    // 현재 활성 제재 (해제 안 됨 + 기간 안 지남)
    @Query("""
            SELECT s FROM UserSanction s
            WHERE s.user.id = :userId
              AND s.releasedAt IS NULL
              AND (s.endedAt IS NULL OR s.endedAt > CURRENT_TIMESTAMP)
            ORDER BY s.createdAt DESC
            """)
    List<UserSanction> findActiveSanctions(@Param("userId") Long userId);

    @Query("""
            SELECT s FROM UserSanction s
            LEFT JOIN FETCH s.processedBy
            LEFT JOIN FETCH s.releasedBy
            WHERE s.id = :id
            """)
    Optional<UserSanction> findByIdWithDetails(@Param("id") Long id);
}
