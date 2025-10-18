package com.jiujitsu.api.domain.community.repository;

import com.jiujitsu.api.domain.community.entity.OwnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, Long> {
}
