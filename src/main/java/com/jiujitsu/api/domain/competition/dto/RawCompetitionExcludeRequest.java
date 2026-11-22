package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.ExcludedReason;
import jakarta.validation.constraints.NotNull;

public record RawCompetitionExcludeRequest(
        @NotNull ExcludedReason reason
) {}
