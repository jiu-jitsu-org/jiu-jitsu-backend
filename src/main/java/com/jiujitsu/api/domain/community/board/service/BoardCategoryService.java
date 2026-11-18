package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.BoardCategoryResponse;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.board.repository.BoardCategoryRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardCategoryService {
    private final BoardCategoryRepository boardCategoryRepository;

    /**
     * 카테고리 목록 조회 — 사용 중인 카테고리만 보이는 순서대로
     */
    @Transactional(readOnly = true)
    public List<BoardCategoryResponse> getCategory() {
        return boardCategoryRepository.findAllByIsActiveTrueOrderByDisplayOrderAscIdAsc()
                .stream()
                .map(BoardCategoryResponse::new)
                .toList();
    }

    /**
     * 카테고리 생성 임시(하드코딩) — 이미 있는 이름은 건너뜀
     */
    public void createCategory() {
        List<BoardCategory> categories = List.of(
                category("도장 · 오픈매트", "도장 고르기, 오픈매트, 승급 · 도장 생활", 1),
                category("대회", "대회 준비, 대회 후기", 2),
                category("훈련 & 기술", "기술 · 포지션, 기술 영상, 훈련 팁", 3),
                category("도복 · 장비", "도복 사이즈, 보호대, 장비 후기", 4),
                category("몸관리 · 부상", "부상 · 회복, 체력 · 체중 관리", 5),
                category("매트 위 수다", "그 밖의 주짓수 이야기", 6)
        );
        boardCategoryRepository.saveAll(categories.stream()
                .filter(category -> !boardCategoryRepository.existsByName(category.getName()))
                .toList());
    }

    private static BoardCategory category(String name, String description, int displayOrder) {
        return BoardCategory.builder()
                .name(name)
                .description(description)
                .displayOrder(displayOrder)
                .isActive(true)
                .build();
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
