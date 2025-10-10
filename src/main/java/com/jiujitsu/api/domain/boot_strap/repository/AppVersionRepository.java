package com.jiujitsu.api.domain.boot_strap.repository;

import com.jiujitsu.api.domain.boot_strap.entity.AppVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface AppVersionRepository extends JpaRepository<AppVersion, Long> {

    Optional<AppVersion> findByOsName(String osName);

}
