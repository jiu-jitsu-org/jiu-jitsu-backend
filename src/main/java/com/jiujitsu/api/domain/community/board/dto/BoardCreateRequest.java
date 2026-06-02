package com.jiujitsu.api.domain.community.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Schema(description = "게시글 생성 요청")
public record BoardCreateRequest(
        @NotNull(message = "카테고리 ID는 필수입니다.")
        @Schema(description = "카테고리 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            Long categoryId,
        @NotBlank(message = "제목은 필수입니다.")
        @Schema(description = "제목", example = "게시글 제목", requiredMode = Schema.RequiredMode.REQUIRED)
            String title,
        @NotBlank(message = "내용은 필수입니다.")
        @Schema(description = "내용", example = "게시글 내용", requiredMode = Schema.RequiredMode.REQUIRED)
            String body,
        @Schema(description = "이미지 파일 ID 리스트")
            List<Long> imageFileIdList
) {
    public List<Long> imageFileIdListOrEmpty() {
        return imageFileIdList == null ? List.of() : imageFileIdList;
    }
}
