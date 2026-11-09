package com.jiujitsu.api.domain.competition.mapper;

import com.jiujitsu.api.domain.competition.dto.CompetitionResponse;
import com.jiujitsu.api.domain.competition.entity.Competition;
import org.springframework.stereotype.Component;

@Component
public class CompetitionMapper {

    public CompetitionResponse toResponse(Competition competition) {
        return new CompetitionResponse(competition.getId(), competition.getName(), competition.getCompetitionYear());
    }
}
