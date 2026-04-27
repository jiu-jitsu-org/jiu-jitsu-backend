package com.jiujitsu.api.domain.community.board.factory;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.file.ImageUrl;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

@Component
public class BoardFactory {
    // entity 생성 클래스(Factory)

    /**
     * Create Content
     */
    public Content createContent(List<String> imagesUrls) {
        Content content = Content.builder()
                .contentType(ContentType.BOARD)
                .build();

        content.addImageUrls(createImageUrls(imagesUrls));

        return content;
    }

    /**
     * Create Board
     */
    public Board createBoard(BoardCategory category, Content content, String title, String body) {
        return Board.builder()
                .category(category)
                .content(content)
                .title(title)
                .body(body)
                .build();
    }


    /**
     * Create ImageUrl Entity
     */
    public List<ImageUrl> createImageUrls(List<String> urls) {
        if (urls == null || urls.isEmpty()) {
            return Collections.emptyList();
        }
        return urls.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(url -> ImageUrl.builder().imageUrl(url).build())
                .toList();
    }
}
