package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.*;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.board.factory.BoardFactory;
import com.jiujitsu.api.domain.community.board.mapper.BoardMapper;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.dto.ContentLikeResponse;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.entity.ContentLike;
import com.jiujitsu.api.domain.community.content.factory.ContentLikeFactory;
import com.jiujitsu.api.domain.community.content.mapper.ContentLikeMapper;
import com.jiujitsu.api.domain.community.content.repository.ContentLikeRepository;
import com.jiujitsu.api.domain.user.entity.User;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BoardService {

    private final AuthenticationFacade authenticationFacade;
    private final BoardRepository boardRepository;
    private final BoardFactory boardFactory;
    private final BoardMapper boardMapper;
    private final BoardCategoryService boardCategoryService;
    private final CommunityCommentsService communityCommentsService;
    private final ContentLikeRepository contentLikeRepository;
    private final ContentLikeFactory contentLikeFactory;
    private final ContentLikeMapper contentLikeMapper;


    /**
     * 게시물 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<BoardListResponse> getList(BoardListRequest boardListRequest, Pageable pageable) {
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
                : communityCommentsService.getContentsComments(contentIds);

        // 좋아요 조회
        Map<Long, Long> contentLikeMap = contentIds.isEmpty()
                ? Map.of()
                : getContentLikeCount(contentIds);

        return page.map(board ->
                boardMapper.toBoardListResponse(
                        board,
                        commentCountMap.getOrDefault(board.getContent().getId(), 0L),
                        contentLikeMap.getOrDefault(board.getContent().getId(), 0L)
                ));
    }

    /**
     * 게시물 상세 조회
     */
    @Transactional(readOnly = true)
    public BoardResponse getById(Long id) {
        // 게시글 조회
        Board board = boardRepository.findByContent_Id(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));
        Content content = board.getContent();

        // 댓글 수 조회
        long commentCount = communityCommentsService.getCountComments(content.getId());

        // 좋아요 수 조회
        long likeCount = contentLikeRepository.countByContent_Id(content.getId());

        // dto 생성
        return boardMapper.toResponse(board, commentCount, likeCount);
    }

    /**
     * 게시물 작성
     */
    @Transactional
    public BoardResponse create(BoardCreateRequest request) {
        // 로그인 유저 체크
        authenticationFacade.checkCurrentUser();

        // 등록 카테고리 조회
        BoardCategory category = boardCategoryService.findActiveCategory(request.categoryId());

        // content 생성
        Content content = boardFactory.createContent(request.imageUrlListOrEmpty());

        // Board 생성
        Board board = boardFactory.createBoard(category, content, request.title(), request.body());
        board = boardRepository.save(board);

        // dto 생성
        return boardMapper.toResponse(board, null, null);
    }

    /**
     * 게시물 수정
     */
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

        // dto 생성
        return boardMapper.toResponse(board, null, null);
    }

    /**
     * 게시물 삭제
     */
    @Transactional
    public void delete(Long id) {
        Board board = boardRepository.findByContent_Id(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        // 권한 체크
        board.validateOwner(authenticationFacade.getCurrentUser());

        boardRepository.delete(board);
    }

    /**
     * 게시물 좋아요 등록/삭제
     */
    @Transactional
    public ContentLikeResponse like(Long id) {
        // 로그인 유저 정보 조회
        User user = authenticationFacade.getCurrentUser();

        // 게시물 조회
        Board board = boardRepository.findByContent_Id(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));
        Content content = board.getContent();

        // 기존 좋아요 조회
        ContentLike newLike = null;
        Optional<ContentLike> existLike = contentLikeRepository.findByContentIdAndCreatedBy(id, user);

        if (existLike.isPresent()) {
            // 좋아요 취소
            contentLikeRepository.delete(existLike.get());
        } else {
            // 좋아요 등록
            newLike = contentLikeFactory.createCommentLike(content);
            contentLikeRepository.save(newLike);
        }

        return contentLikeMapper.toContentLikeResponse(content, newLike);
    }

    /**
     * 게시글 목록 > 좋아요 카운트 카운트 조회
     */
    public Map<Long, Long> getContentLikeCount(List<Long> contentIds) {
        return contentLikeRepository.countContentLikeByContentIds(contentIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
    }
}
