package com.jiujitsu.api.domain.file.dto;

import com.jiujitsu.api.domain.file.ImageFile;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "이미지 정보")
public record ImageInfo(
        @Schema(description = "이미지 파일 ID") Long id,
        @Schema(description = "이미지 URL") String imageUrl
) {
    public static ImageInfo from(ImageFile imageFile) {
        if (imageFile == null) return null;
        return new ImageInfo(imageFile.getId(), imageFile.getImageUrl());
    }
}
