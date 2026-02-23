package com.jiujitsu.api.domain.community.profile.repository;

import com.jiujitsu.api.domain.community.profile.entity.OwnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, Long> {
}
