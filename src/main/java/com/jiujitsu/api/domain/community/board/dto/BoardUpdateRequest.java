package com.jiujitsu.api.domain.community.board.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
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

    @Size(max = 3, message = "이미지는 최대 3장까지 첨부할 수 있습니다.")
    @Schema(description = "이미지 파일 ID 리스트 (전체)")
    private List<Long> imageFileIdList;

    @Schema(description = "태그 목록", example = "[\"BJJ\", \"운동\"]")
    private List<String> tags;

    // 다른 칸과 달리 null 도 값이다 (보낸 값으로 교체) — 수정 화면은 항상 현재 연결을 다시 보낸다
    @Schema(description = "연결할 대회 ID. 보낸 값으로 교체하며 null·생략이면 연결 해제 (공개 대회만, 지금 연결된 대회는 그대로 둘 수 있음)", example = "1")
    private Long competitionId;
}
