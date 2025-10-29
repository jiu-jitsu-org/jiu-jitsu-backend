package com.jiujitsu.api.domain.community.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Getter;

@Getter
public class AcademyUpdateRequest {
        @Size(max = 100)
        @Schema(description = "소속 도장명", example = "Gracie Barra Seoul")
        String academyName;
}
