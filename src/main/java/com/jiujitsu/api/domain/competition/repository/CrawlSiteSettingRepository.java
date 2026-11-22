package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CrawlSite;
import com.jiujitsu.api.domain.competition.entity.CrawlSiteSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CrawlSiteSettingRepository extends JpaRepository<CrawlSiteSetting, CrawlSite> {

    Optional<CrawlSiteSetting> findBySite(CrawlSite site);
}
