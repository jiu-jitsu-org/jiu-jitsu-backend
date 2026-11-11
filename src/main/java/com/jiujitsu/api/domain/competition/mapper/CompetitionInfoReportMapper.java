package com.jiujitsu.api.domain.competition.mapper;

import com.jiujitsu.api.domain.competition.dto.AdminCompetitionInfoReportResponse;
import com.jiujitsu.api.domain.competition.entity.CompetitionInfoReport;
import org.springframework.stereotype.Component;

@Component
public class CompetitionInfoReportMapper {

    public AdminCompetitionInfoReportResponse toAdminResponse(CompetitionInfoReport report) {
        return new AdminCompetitionInfoReportResponse(
                report.getId(),
                report.getCompetition().getId(),
                report.getCompetition().getName(),
                report.getCompetition().getCompetitionYear(),
                report.getField(),
                report.getDetail(),
                report.getUser().getId(),
                report.getUser().getNickname(),
                report.getCreatedAt()
        );
    }
}
