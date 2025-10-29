package com.jiujitsu.api.domain.community.dto;

import com.jiujitsu.api.domain.community.entity.PositionType;
import com.jiujitsu.api.domain.community.entity.SubmissionType;
import com.jiujitsu.api.domain.community.entity.TechniqueType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class TechniqueUpdateRequest {
    @NotNull
    @Schema(description = "특기/최애 구분", example = "BEST")
    private PreferenceType preferenceType;

    @Schema(description = "서브미션", example = "CHOKES")
    private SubmissionType submission;

    @Schema(description = "기술", example = "SWEEPS")
    private TechniqueType techniqueType;

    @Schema(description = "포지션", example = "TOP")
    private PositionType position;

    public enum PreferenceType {
        BEST,
        FAVORITE
    }
}
