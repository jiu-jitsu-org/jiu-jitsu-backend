package com.jiujitsu.api.domain.community.dto;

import com.jiujitsu.api.domain.community.entity.BeltRank;
import com.jiujitsu.api.domain.community.entity.BeltStripe;
import com.jiujitsu.api.domain.community.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class LevelUpdateRequest {
        @NotNull
        @Schema(description = "벨트 등급", example = "BLUE")
        BeltRank beltRank;

        @NotNull
        @Schema(description = "그랄(0~4)", example = "STRIPE_2")
        BeltStripe beltStripe;

        @NotNull
        @Schema(description = "성별", example = "MALE")
        Gender gender;

        @Digits(integer = 5, fraction = 1)
        @DecimalMin(value = "0.0", inclusive = false)
        @DecimalMax(value = "300.0", inclusive = true)
        @Schema(description = "체급(kg, 소수점 1자리)", example = "76.5")
        Double weightKg;

        @Schema(description = "체급 숨기기 여부")
        Boolean isWeightHidden;
}
