package com.jiujitsu.api.domain.community.board.mapper;

import com.jiujitsu.api.domain.community.board.dto.BoardListResponse;
import com.jiujitsu.api.domain.community.board.dto.BoardResponse;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.file.ImageUrl;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BoardMapper {

    /**
     * 목록 response
     */
    public BoardListResponse toBoardListResponse(Board board, long commentCount) {
        Content content = board.getContent();

        return new BoardListResponse(
                board.getId(),
                content.getId(),
                board.getCategory().getId(),
                board.getCategory().getName(),
                board.getTitle(),
                board.getBody(),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                commentCount,
                getImageUrlStrings(content)
        );
    }
    public BoardResponse toResponse(Board board, long commentCount) {
        Content content = board.getContent();
        return new BoardResponse(
                board.getId(),
                content.getId(),
                board.getCategory().getId(),
                board.getCategory().getName(),
                board.getTitle(),
                board.getBody(),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                commentCount,
                getImageUrlStrings(content)
        );
    }

    private List<String> getImageUrlStrings(Content content) {
        return content.getImageUrls().stream()
                .map(ImageUrl::getImageUrl)
                .toList();
    }
}
