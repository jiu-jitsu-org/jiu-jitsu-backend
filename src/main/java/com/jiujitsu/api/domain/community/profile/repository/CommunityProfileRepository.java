package com.jiujitsu.api.domain.community.profile.repository;

import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommunityProfileRepository extends JpaRepository<CommunityProfile, Long> {
    Optional<CommunityProfile> findByUserId(Long userId);
    Optional<CommunityProfile> findByUser(User user);
}
