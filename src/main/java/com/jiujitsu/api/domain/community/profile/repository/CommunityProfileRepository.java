package com.jiujitsu.api.domain.community.profile.repository;

import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CommunityProfileRepository extends JpaRepository<CommunityProfile, Long> {
    Optional<CommunityProfile> findByUser(User user);

    @Query("SELECT cp FROM CommunityProfile cp WHERE cp.user.id IN :userIds")
    List<CommunityProfile> findAllByUserIdIn(@Param("userIds") Collection<Long> userIds);
}
