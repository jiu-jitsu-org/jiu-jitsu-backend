package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.BoardCategoryResponse;
import com.jiujitsu.api.domain.community.board.dto.BoardCreateRequest;
import com.jiujitsu.api.domain.community.board.dto.BoardResponse;
import com.jiujitsu.api.domain.community.board.dto.BoardUpdateRequest;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.board.repository.BoardCategoryRepository;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<BoardCategoryResponse> getCategory() {
        return boardCategoryRepository.findAll()
                .stream()
                .map(BoardCategoryResponse::new)
                .toList();
    }
    @Transactional
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

    @Transactional
    public BoardResponse create(BoardCreateRequest request) {
        // todo: 사용자 인증 부분 체크하는거 공통 처리 필요
        Long userId = AuthenticationUtil.getCurrentUserId()
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        BoardCategory category = boardCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_CATEGORY_NOT_FOUND));
        if (!category.isActive()) {
            throw new ErrorException(ErrorCode.BOARD_CATEGORY_NOT_FOUND);
        }

        Content content = contentRepository.save(
                Content.builder().contentType(ContentType.BOARD).build()
        );
        Board board = boardRepository.save(
                Board.builder()
                        .category(category)
                        .content(content)
                        .title(request.getTitle())
                        .body(request.getBody())
                        .build()
        );
        return toResponse(board);
    }

    @Transactional(readOnly = true)
    public BoardResponse getById(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));
        return toResponse(board);
    }

    @Transactional(readOnly = true)
    public Page<BoardResponse> getList(Long categoryId, Pageable pageable) {
        Page<Board> page = categoryId != null
                ? boardRepository.findAllByCategory_Id(categoryId, pageable)
                : boardRepository.findAll(pageable);
        return page.map(this::toResponse);
    }

    @Transactional
    public BoardResponse update(Long id, BoardUpdateRequest request) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        Long userId = AuthenticationUtil.getCurrentUserId()
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        if (!Objects.equals(user, board.getCreatedBy())) {
            throw new ErrorException(ErrorCode.PERMISSION_DENIED);
        }

        if (request.getCategoryId() != null) {
            BoardCategory category = boardCategoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_CATEGORY_NOT_FOUND));
            if (!category.isActive()) {
                throw new ErrorException(ErrorCode.BOARD_CATEGORY_NOT_FOUND);
            }
            board.changeCategory(category);
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            board.changeTitle(request.getTitle());
        }
        if (request.getBody() != null && !request.getBody().isBlank()) {
            board.changeBody(request.getBody());
        }

        return toResponse(boardRepository.save(board));
    }

    @Transactional
    public void delete(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        Long userId = AuthenticationUtil.getCurrentUserId()
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        if (!Objects.equals(user, board.getCreatedBy())) {
            throw new ErrorException(ErrorCode.PERMISSION_DENIED);
        }

        Content content = board.getContent();
        boardRepository.delete(board);
        contentRepository.delete(content);
    }

    private BoardResponse toResponse(Board board) {
        return BoardResponse.builder()
                .id(board.getId())
                .contentId(board.getContent().getId())
                .categoryId(board.getCategory().getId())
                .categoryName(board.getCategory().getName())
                .title(board.getTitle())
                .body(board.getBody())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .build();
    }
}
