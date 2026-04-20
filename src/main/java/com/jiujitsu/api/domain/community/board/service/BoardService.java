package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.BoardCreateRequest;
import com.jiujitsu.api.domain.community.board.dto.BoardListRequest;
import com.jiujitsu.api.domain.community.board.dto.BoardResponse;
import com.jiujitsu.api.domain.community.board.dto.BoardUpdateRequest;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.board.factory.BoardFactory;
import com.jiujitsu.api.domain.community.board.mapper.BoardMapper;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final BoardRepository boardRepository;
    private final BoardCategoryService boardCategoryService;
    private final ContentRepository contentRepository;
    private final CommunityCommentsService communityCommentsService;
    private final AuthenticationFacade authenticationFacade;
    private final BoardFactory boardFactory;
    private final BoardMapper boardMapper;

    @Transactional
    public BoardResponse create(BoardCreateRequest request) {
        // 로그인 유저 체크
        authenticationFacade.checkCurrentUser();

        // 등록 카테고리 조회
        BoardCategory category = boardCategoryService.findActiveCategory(request.getCategoryId());

        // content 생성
        Content content = boardFactory.createContent(request.getImageUrlList());

        // Board 생성
        Board board = boardFactory.createBoard(category, content, request.getTitle(), request.getBody());
        board = boardRepository.save(board);

        // 댓글 수 조회
        long commentCount = communityCommentsService.getCountComments(content.getId());

        // dto 생성
        return boardMapper.toResponse(board, commentCount);
    }

    @Transactional(readOnly = true)
    public BoardResponse getById(Long id) {
        // 게시글 조회
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        // 댓글 수 조회
        long commentCount = communityCommentsService.getCountComments(board.getContent().getId());

        // dto 생성
        return boardMapper.toResponse(board, commentCount);
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
                : communityCommentsService.getContentComments(contentIds);

        return page.map(board -> boardMapper.toResponse(board, commentCountMap.getOrDefault(board.getContent().getId(), 0L)));
    }

    @Transactional
    public BoardResponse update(Long id, BoardUpdateRequest request) {
        // 게시물 조회
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        // 권한 체크
        board.validateOwner(authenticationFacade.getCurrentUser());

        if (request.getCategoryId() != null) {
            board.changeCategory(boardCategoryService.findActiveCategory(request.getCategoryId()));
        }
        if (request.getTitle() != null && !request.getTitle().isBlank()) {
            board.changeTitle(request.getTitle());
        }
        if (request.getBody() != null && !request.getBody().isBlank()) {
            board.changeBody(request.getBody());
        }

        // 기존 이미지 모두 제거 후 재생성 (orphanRemoval 로 자동 삭제)
        Content content = board.getContent();
        content.getImageUrls().clear();
        content.addImageUrls(boardFactory.createImageUrls(request.getImageUrlList()));


        // 댓글 수 조회
        long commentCount = communityCommentsService.getCountComments(board.getContent().getId());

        return boardMapper.toResponse(board, commentCount);
    }

    @Transactional
    public void delete(Long id) {
        Board board = boardRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        // 권한 체크
        board.validateOwner(authenticationFacade.getCurrentUser());

        boardRepository.delete(board);
    }
}
