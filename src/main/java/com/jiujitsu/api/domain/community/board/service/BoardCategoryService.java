package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.BoardCategoryResponse;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.board.repository.BoardCategoryRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardCategoryService {
    private final BoardCategoryRepository boardCategoryRepository;

    /**
     * 카테고리 목록 조회
     */
    @Transactional(readOnly = true)
    public List<BoardCategoryResponse> getCategory() {
        return boardCategoryRepository.findAll()
                .stream()
                .map(BoardCategoryResponse::new)
                .toList();
    }

    /**
     * 카테고리 생성 임시(하드코딩)
     */
    public void createCategory() {
        BoardCategory category1 = BoardCategory.builder()
                .name("매트 위 수다")
                .isActive(true)
                .build();

        BoardCategory category2 = BoardCategory.builder()
                .name("훈련 & 기술")
                .isActive(true)
                .build();

        BoardCategory category3 = BoardCategory.builder()
                .name("도장")
                .isActive(true)
                .build();

        BoardCategory category4 = BoardCategory.builder()
                .name("장비")
                .isActive(true)
                .build();

        BoardCategory category5 = BoardCategory.builder()
                .name("대회")
                .isActive(true)
                .build();

        boardCategoryRepository.saveAll(Arrays.asList(
                category1, category2, category3, category4, category5
        ));
    }

    public BoardCategory findActiveCategory(Long categoryId) {
        BoardCategory category = boardCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_CATEGORY_NOT_FOUND));
        if (!category.isActive()) {
            throw new ErrorException(ErrorCode.BOARD_CATEGORY_NOT_FOUND);
        }
        return category;
    }
}
