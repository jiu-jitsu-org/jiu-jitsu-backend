package com.jiujitsu.api.domain.competition.repository;

import com.jiujitsu.api.domain.competition.entity.CrawlHeldValue;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CrawlHeldValueRepository extends JpaRepository<CrawlHeldValue, Long> {

    List<CrawlHeldValue> findAllByCompetitionId(Long competitionId);

    boolean existsByCompetitionIdAndFieldNameAndHeldCrawlValue(
            Long competitionId, String fieldName, String heldCrawlValue);

    void deleteByCompetitionIdAndFieldName(Long competitionId, String fieldName);
}
