package com.jiujitsu.api.domain.user.repository;

import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.user.entity.SnsProvider;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    Optional<User> findBySnsProviderAndSnsId(SnsProvider snsProvider, String snsId);
    //todo: 테스트용 삭제(findBySnsProviderAndNickname)
    Optional<User> findBySnsProviderAndNickname(SnsProvider snsProvider, String nickname);

    Optional<User> findByEmail(String email);
    
    boolean existsByEmail(String email);
    
    boolean existsBySnsProviderAndSnsId(SnsProvider snsProvider, String snsId);
    
    @Query("SELECT u FROM User u WHERE u.status = :status AND u.deletedAt < :date")
    List<User> findByStatusAndDeletedAtBefore(
        @Param("status") UserStatus status,
        @Param("date") LocalDateTime date
    );
    
    Optional<User> findByIdAndStatus(Long id, UserStatus status);

    Optional<User> findByNickname(String nickname);

    Optional<User> findByEmailAndRole(String email, UserRole role);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.appInfos WHERE u.id = :id")
    Optional<User> findByIdWithAppInfos(@Param("id") Long id);

    Optional<User> findByProfileImageFile(ImageFile imageFile);

    /**
     * 같은 유저의 동시 요청 직렬화용 행 잠금 (관장/사범 인증 신청 중복 생성 방지)
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT u FROM User u WHERE u.id = :id")
    Optional<User> findByIdForUpdate(@Param("id") Long id);
}
