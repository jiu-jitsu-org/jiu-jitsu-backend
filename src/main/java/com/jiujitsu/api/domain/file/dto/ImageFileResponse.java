package com.jiujitsu.api.domain.file.dto;

import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.file.ImageFileStatus;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이미지 파일 등록 응답")
public record ImageFileResponse(
        @Schema(description = "이미지 파일 ID") Long id,
        @Schema(description = "CDN 파일 ID") String cdnId,
        @Schema(description = "이미지 URL") String imageUrl,
        @Schema(description = "이미지 상태 (TEMP/ACTIVE)") ImageFileStatus status
) {
    public static ImageFileResponse from(ImageFile imageFile) {
        return new ImageFileResponse(
                imageFile.getId(),
                imageFile.getCdnId(),
                imageFile.getImageUrl(),
                imageFile.getStatus()
        );
    }
}
