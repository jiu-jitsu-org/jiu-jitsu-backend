package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.*;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.board.repository.BoardCategoryRepository;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCategoryRepository boardCategoryRepository;
    private final ContentRepository contentRepository;
    private final UserRepository userRepository;
    private final CommunityCommentsRepository communityCommentsRepository;
    private final AuthenticationFacade authenticationFacade;

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
        // 로그인 유저 체크
        authenticationFacade.checkCurrentUser();

        // 카테고리 확인
        BoardCategory category = boardCategoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_CATEGORY_NOT_FOUND));
        if (!category.isActive()) {
            throw new ErrorException(ErrorCode.BOARD_CATEGORY_NOT_FOUND);
        }

        // 컨텐츠(게시물) 저장
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
        return toResponse(board, communityCommentsRepository.countByContent_IdAndParentIdIsNull(board.getContent().getId()));
    }

    @Transactional(readOnly = true)
    public BoardResponse getById(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));
        return toResponse(board, communityCommentsRepository.countByContent_IdAndParentIdIsNull(board.getContent().getId()));
    }

    @Transactional(readOnly = true)
    public Page<BoardResponse> getList(BoardListRequest boardListRequest, Pageable pageable) {
        Long categoryId = boardListRequest.getCategoryId();

        // 게시판 조회
        Page<Board> page = categoryId != null
                ? boardRepository.findAllByCategory_Id(categoryId, pageable)
                : boardRepository.findAll(pageable);

        // 컨텐츠(공통) 조회
        List<Long> contentIds = page.getContent().stream()
                .map(b -> b.getContent().getId())
                .toList();

        // 댓글 조회
        Map<Long, Long> commentCountMap = contentIds.isEmpty()
                ? Map.of()
                : communityCommentsRepository.countTopLevelCommentsByContentIds(contentIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));

        return page.map(board -> toResponse(board, commentCountMap.getOrDefault(board.getContent().getId(), 0L)));
    }

    @Transactional
    public BoardResponse update(Long id, BoardUpdateRequest request) {
        // 게시물 조회
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        // 로그인 유저 조회
        User user = authenticationFacade.getCurrentUser();

        // 권한 체크
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

        Board saved = boardRepository.save(board);
        return toResponse(saved, communityCommentsRepository.countByContent_IdAndParentIdIsNull(saved.getContent().getId()));
    }

    @Transactional
    public void delete(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        User user = authenticationFacade.getCurrentUser();

        if (!Objects.equals(user, board.getCreatedBy())) {
            throw new ErrorException(ErrorCode.PERMISSION_DENIED);
        }

        Content content = board.getContent();
        boardRepository.delete(board);
        contentRepository.delete(content);
    }

    private BoardResponse toResponse(Board board, long commentCount) {
        return BoardResponse.builder()
                .id(board.getId())
                .contentId(board.getContent().getId())
                .categoryId(board.getCategory().getId())
                .categoryName(board.getCategory().getName())
                .title(board.getTitle())
                .body(board.getBody())
                .createdAt(board.getCreatedAt())
                .updatedAt(board.getUpdatedAt())
                .commentCount(commentCount)
                .build();
    }
}
