package com.jiujitsu.api.domain.community.board.mapper;

import com.jiujitsu.api.domain.community.board.dto.BoardResponse;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.file.ImageUrl;
import org.springframework.stereotype.Component;

@Component
public class BoardMapper {
    public BoardResponse toResponse(Board board, long commentCount) {
        Content content = board.getContent();
        return BoardResponse.builder()
                .id(board.getId())
                .contentId(content.getId())
                .categoryId(board.getCategory().getId())
                .categoryName(board.getCategory().getName())
                .title(board.getTitle())
                .body(board.getBody())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .commentCount(commentCount)
                .imageUrlList(
                        content.getImageUrls()
                                .stream()
                                .map(ImageUrl::getImageUrl)
                                .toList()
                )
                .build();
    }
}
