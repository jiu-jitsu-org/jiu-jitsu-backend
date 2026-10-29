package com.jiujitsu.api.domain.user.repository;

import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.user.entity.OwnerVerificationRequest;
import com.jiujitsu.api.domain.user.entity.OwnerVerificationStatus;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface OwnerVerificationRequestRepository extends JpaRepository<OwnerVerificationRequest, Long> {

    /**
     * 유저의 최신 신청
     */
    Optional<OwnerVerificationRequest> findTopByUserOrderByIdDesc(User user);

    /**
     * 관리자 목록: 상태별, 신청 최신순 (정렬은 쿼리에 고정)
     */
    @Query(value = """
            SELECT r FROM OwnerVerificationRequest r
            JOIN FETCH r.user u
            LEFT JOIN FETCH u.profileImageFile
            LEFT JOIN FETCH r.documentImageFile
            WHERE r.status = :status
            ORDER BY r.createdAt DESC, r.id DESC
            """,
            countQuery = "SELECT COUNT(r) FROM OwnerVerificationRequest r WHERE r.status = :status")
    Page<OwnerVerificationRequest> findByStatusWithUser(@Param("status") OwnerVerificationStatus status, Pageable pageable);

    List<OwnerVerificationRequest> findByDocumentImageFile(ImageFile imageFile);

    /**
     * 승인·거절 동시 처리 방지용 행 잠금
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT r FROM OwnerVerificationRequest r WHERE r.id = :id")
    Optional<OwnerVerificationRequest> findByIdForUpdate(@Param("id") Long id);
}
