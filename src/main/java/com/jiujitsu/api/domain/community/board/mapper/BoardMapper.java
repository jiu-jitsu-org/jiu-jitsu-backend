package com.jiujitsu.api.domain.community.board.mapper;

import com.jiujitsu.api.domain.community.board.dto.BoardListResponse;
import com.jiujitsu.api.domain.community.board.dto.BoardResponse;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardTag;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.profile.dto.CommunityProfileInfo;
import com.jiujitsu.api.domain.file.dto.ImageInfo;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BoardMapper {

    /**
     * 목록 response
     */
    public BoardListResponse toBoardListResponse(Board board, long commentCount, long likeCount, boolean isCommented, boolean isLiked, boolean isSaved, boolean isAuthor) {
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
                content.getViewCount(),
                isCommented,
                isLiked,
                isSaved,
                getImageInfoList(content),
                getTagNames(board),
                CommunityProfileInfo.from(board.getCreatedBy()),
                isAuthor
        );
    }

    /**
     * 상세 response
     */
    public BoardResponse toResponse(Board board, Long commentCount, Long likeCount, boolean isCommented, boolean isLiked, boolean isSaved, Boolean noticeEnabled, boolean isAuthor) {
        Content content = board.getContent();
        return new BoardResponse(
                content.getId(),
                board.getCategory().getId(),
                board.getCategory().getName(),
                board.getTitle(),
                board.getBody(),
                board.getCreatedAt(),
                board.getUpdatedAt(),
                board.isUpdated(),
                commentCount,
                likeCount,
                content.getViewCount(),
                isCommented,
                isLiked,
                isSaved,
                getImageInfoList(content),
                getTagNames(board),
                noticeEnabled,
                CommunityProfileInfo.from(board.getCreatedBy()),
                isAuthor
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
                board.isUpdated(),
                null,
                null,
                content.getViewCount(),
                null,
                null,
                null,
                getImageInfoList(content),
                getTagNames(board),
                null,
                CommunityProfileInfo.from(board.getCreatedBy()),
                true
        );
    }

    private List<ImageInfo> getImageInfoList(Content content) {
        return content.getImageFiles().stream()
                .map(ImageInfo::from)
                .toList();
    }

    private List<String> getTagNames(Board board) {
        return board.getTags().stream()
                .map(BoardTag::getName)
                .toList();
    }
}
