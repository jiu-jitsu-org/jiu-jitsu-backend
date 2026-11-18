package com.jiujitsu.api.domain.community.board.dto;

import com.jiujitsu.api.domain.community.board.entity.Board;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

@Schema(description = "게시글 생성 요청")
public record BoardCreateRequest(
        @NotNull(message = "카테고리 ID는 필수입니다.")
        @Schema(description = "카테고리 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            Long categoryId,
        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = Board.TITLE_MAX_LENGTH, message = "제목은 45자 이하여야 합니다.")
        @Schema(description = "제목 (최대 45자)", example = "게시글 제목", maxLength = Board.TITLE_MAX_LENGTH, requiredMode = Schema.RequiredMode.REQUIRED)
            String title,
        @Size(max = Board.BODY_MAX_LENGTH, message = "내용은 3,000자 이하여야 합니다.")
        @Schema(description = "내용 (선택, 최대 3,000자). 생략 · 빈 값 · 공백만이면 \"\" 로 저장", example = "게시글 내용", maxLength = Board.BODY_MAX_LENGTH)
            String body,
        @Size(max = 3, message = "이미지는 최대 3장까지 첨부할 수 있습니다.")
        @Schema(description = "이미지 파일 ID 리스트")
            List<Long> imageFileIdList,
        @Schema(description = "태그 목록", example = "[\"BJJ\", \"운동\"]")
            List<String> tags,
        @Schema(description = "연결할 대회 ID (선택, 공개 대회만). 카테고리와 상관없이 받음 — 대회 고르기는 GET /competitions/picker")
            Long competitionId
) {
    public List<Long> imageFileIdListOrEmpty() {
        return imageFileIdList == null ? List.of() : imageFileIdList;
    }

    // 내용은 선택 — 없는 글은 null 대신 "" 로 저장해 응답 body 가 항상 문자열이게 한다 (#264)
    public String bodyOrEmpty() {
        return body == null || body.isBlank() ? "" : body;
    }

    public List<String> tagsOrEmpty() {
        return tags == null ? List.of() : tags;
    }
}
