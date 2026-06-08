package com.jiujitsu.api.domain.community.board.factory;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.file.repository.ImageFileRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class BoardFactory {

    private final ImageFileRepository imageFileRepository;

    public Content createContent(List<Long> imageFileIds) {
        Content content = Content.builder()
                .contentType(ContentType.BOARD)
                .build();
        content.addImageFiles(createImageFiles(imageFileIds));
        return content;
    }

    public Board createBoard(BoardCategory category, Content content, String title, String body) {
        return Board.builder()
                .category(category)
                .content(content)
                .title(title)
                .body(body)
                .build();
    }

    public List<ImageFile> createImageFiles(List<Long> imageFileIds) {
        if (imageFileIds == null || imageFileIds.isEmpty()) {
            return Collections.emptyList();
        }
        return imageFileIds.stream()
                .map(id -> imageFileRepository.findById(id)
                        .orElseThrow(() -> new ErrorException(ErrorCode.IMAGE_FILE_NOT_FOUND)))
                .toList();
    }
}
