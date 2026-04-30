package com.jiujitsu.api.domain.community.content.repository;

import com.jiujitsu.api.domain.community.content.entity.ContentSave;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface ContentSaveRepository extends JpaRepository<ContentSave, Long> {
    Optional<ContentSave> findByContentIdAndCreatedBy(Long id, User user);

    @Query("""
            select distinct c.content.id
            from ContentSave c
            where c.createdBy.id = :userId
              and c.content.id in :contentIds
            """)
    Set<Long> findUserSavedContentIds(@Param("userId") Long userId, @Param("contentIds") List<Long> contentIds);
}
