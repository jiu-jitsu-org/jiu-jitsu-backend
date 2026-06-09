package com.jiujitsu.api.domain.community.board.mapper;

import com.jiujitsu.api.domain.community.board.dto.BoardListResponse;
import com.jiujitsu.api.domain.community.board.dto.BoardResponse;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

@Component
public class BoardMapper {

    /**
     * 목록 response
     */
    public BoardListResponse toBoardListResponse(Board board, long commentCount, long likeCount, boolean isCommented, boolean isLiked, boolean isSaved) {
        Content content = board.getContent();

        return new BoardListResponse(
                content.getId(),
                board.getCategory().getId(),
                board.getCategory().getName(),
                board.getTitle(),
                board.getBody(),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                commentCount,
                likeCount,
                isCommented,
                isLiked,
                isSaved,
                getImageInfoList(content)
        );
    }

    /**
     * 상세 response
     */
    public BoardResponse toResponse(Board board, Long commentCount, Long likeCount, boolean isCommented, boolean isLiked, boolean isSaved, Boolean noticeEnabled) {
        Content content = board.getContent();
        return new BoardResponse(
                content.getId(),
                board.getCategory().getId(),
                board.getCategory().getName(),
                board.getTitle(),
                board.getBody(),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                !Objects.equals(board.getCreatedAt(), board.getUpdatedAt()),
                commentCount,
                likeCount,
                isCommented,
                isLiked,
                isSaved,
                getImageInfoList(content),
                noticeEnabled
        );
    }

    public BoardResponse toResponse(Board board) {
        Content content = board.getContent();
        return new BoardResponse(
                content.getId(),
                board.getCategory().getId(),
                board.getCategory().getName(),
                board.getTitle(),
                board.getBody(),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                !Objects.equals(board.getCreatedAt(), board.getUpdatedAt()),
                null,
                null,
                null,
                null,
                null,
                getImageInfoList(content),
                null
        );
    }

    private List<ImageInfo> getImageInfoList(Content content) {
        return content.getImageFiles().stream()
                .map(ImageInfo::from)
                .toList();
    }
}
