package com.jiujitsu.api.domain.file.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "이미지 파일 등록 요청")
public record ImageFileRegisterRequest(
        @Schema(description = "CDN 파일 ID", example = "abc123xyz")
        @NotBlank String cdnId,

        @Schema(description = "CDN 이미지 URL", example = "https://ik.imagekit.io/xxx/photo.jpg")
        @NotBlank String imageUrl
) {
}
