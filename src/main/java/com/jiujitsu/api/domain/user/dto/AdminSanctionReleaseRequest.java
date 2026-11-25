package com.jiujitsu.api.domain.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AdminSanctionReleaseRequest(
        @NotBlank
        @Size(max = 500)
        String reason
) {}
