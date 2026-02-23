package com.jiujitsu.api.domain.community.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "게시글 생성 요청")
public class BoardCreateRequest {

    @NotNull(message = "카테고리 ID는 필수입니다.")
    @Schema(description = "카테고리 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long categoryId;

    @NotBlank(message = "제목은 필수입니다.")
    @Schema(description = "제목", example = "게시글 제목", requiredMode = Schema.RequiredMode.REQUIRED)
    private String title;

    @NotBlank(message = "내용은 필수입니다.")
    @Schema(description = "내용", example = "게시글 내용", requiredMode = Schema.RequiredMode.REQUIRED)
    private String body;
}
