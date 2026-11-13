package com.jiujitsu.api.domain.competition.dto;

import com.jiujitsu.api.domain.competition.entity.CompetitionLink;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "상태 링크 주소 (관리자)")
public record CompetitionLinkRequest(
        @NotBlank
        @Size(max = CompetitionLink.URL_MAX_LENGTH)
        @Pattern(regexp = "^\\s*https?://.*", message = "링크는 http:// 또는 https:// 로 시작해야 합니다.")
        @Schema(description = "접수 사이트 페이지 주소 (http·https)", example = "https://www.flowcomp.co.kr/championship/53/register", requiredMode = Schema.RequiredMode.REQUIRED)
            String url
) {
    public String trimmedUrl() {
        return url.strip();
    }
}
