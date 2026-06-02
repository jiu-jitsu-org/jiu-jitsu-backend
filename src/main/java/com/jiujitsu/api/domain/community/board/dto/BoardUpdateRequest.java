package com.jiujitsu.api.domain.community.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "게시글 수정 요청")
public class BoardUpdateRequest {

    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;

    @Schema(description = "제목", example = "수정된 제목")
    private String title;

    @Schema(description = "내용", example = "수정된 내용")
    private String body;

    @Schema(description = "이미지 파일 ID 리스트 (전체)")
    private List<Long> imageFileIdList;
}
