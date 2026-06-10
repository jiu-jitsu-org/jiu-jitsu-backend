package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.*;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.board.entity.BoardListType;
import com.jiujitsu.api.domain.community.board.factory.BoardFactory;
import com.jiujitsu.api.domain.community.board.mapper.BoardMapper;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final AuthenticationFacade authenticationFacade;
    private final BoardRepository boardRepository;
    private final BoardFactory boardFactory;
    private final BoardMapper boardMapper;
    private final BoardCategoryService boardCategoryService;
    private final CommunityCommentsService communityCommentsService;
    private final ContentService contentService;
    private final UserBlockService userBlockService;
    private final NoticeService noticeService;

    /**
     * 게시물 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<BoardListResponse> getList(BoardListRequest boardListRequest, Pageable pageable) {
        BoardListType boardListType = boardListRequest.boardListType();
        List<Long> blockedUserIds = userBlockService.getBlockedUserIds();

        // 게시판 조회
        Page<Board> page = switch (boardListType) {
            case CATEGORY -> {
                Long categoryId = boardListRequest.categoryId();
                yield blockedUserIds.isEmpty()
                        ? boardRepository.findAllByCategory_Id(categoryId, pageable)
                        : boardRepository.findAllByCategoryExcludingBlockedUsers(categoryId, blockedUserIds, pageable);
            }
            case SEARCH -> {
                String keyword = StringUtils.trimToEmpty(boardListRequest.searchKeyword()).toUpperCase(Locale.ROOT);
                yield blockedUserIds.isEmpty()
                        ? boardRepository.findByTitleBodyKeyword(keyword, pageable)
                        : boardRepository.findByTitleBodyKeywordExcludingBlockedUsers(keyword, blockedUserIds, pageable);
            }
            default -> blockedUserIds.isEmpty()
                    ? boardRepository.findAll(pageable)
                    : boardRepository.findAllExcludingBlockedUsers(blockedUserIds, pageable);
        };

        return mapBoardsToResponse(page);
    }

    /**
     * 게시물 상세 조회
     */
    @Transactional(readOnly = true)
    public BoardResponse getById(Long id) {
        // 게시글 조회
        Board board = boardRepository.findByContent_Id(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));
        Long contentId = board.getContent().getId();

        // 댓글 수 조회
        long commentCount = communityCommentsService.getCountComments(contentId);

        // 좋아요 수 조회
        long likeCount = contentService.getContentLikeCount(List.of(contentId))
                .getOrDefault(contentId, 0L);

        // 설정 여부 확인
        Set<Long> commentedContentIds = new HashSet<>();
        Set<Long> likedContentIds = new HashSet<>();
        Set<Long> savedContentIds = new HashSet<>();

        Boolean noticeEnabled = null;
        Optional<User> user = authenticationFacade.getCurrentUserOptional();
        if (user.isPresent()) {
            commentedContentIds = communityCommentsService.getUserCommentedContentIds(user.get().getId(), List.of(contentId));
            likedContentIds = contentService.getUserLikedContentIds(user.get().getId(), List.of(contentId));
            savedContentIds = contentService.getUserSavedContentIds(user.get().getId(), List.of(contentId));
            noticeEnabled = noticeService.isContentNoticeEnabled(user.get().getId(), contentId);
        }

        // dto 생성
        return boardMapper.toResponse(
                board,
                commentCount,
                likeCount,
                commentedContentIds.contains(contentId),
                likedContentIds.contains(contentId),
                savedContentIds.contains(contentId),
                noticeEnabled,
                Objects.equals(board.getCreatedBy(), user.orElse(null))
                );
    }

    /**
     * 게시물 작성
     */
    public BoardResponse create(BoardCreateRequest request) {
        // 로그인 유저 체크
        authenticationFacade.checkCurrentUser();

        // 등록 카테고리 조회
        BoardCategory category = boardCategoryService.findActiveCategory(request.categoryId());

        // content 생성
        Content content = boardFactory.createContent(request.imageFileIdListOrEmpty());

        // Board 생성
        Board board = boardFactory.createBoard(category, content, request.title(), request.body());
        board = boardRepository.save(board);

        // dto 생성
        return boardMapper.toResponse(board);
    }

    /**
     * 게시물 수정
     */
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
        content.getImageFiles().clear();
        content.addImageFiles(boardFactory.createImageFiles(request.getImageFileIdList()));

        // 수정여부 업데이트를 위한 flush 강제호출
        boardRepository.flush();

        // dto 생성
        return boardMapper.toResponse(board);
    }

    /**
     * 게시물 삭제
     */
    public void delete(Long id) {
        Board board = boardRepository.findByContent_Id(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        // 권한 체크
        board.validateOwner(authenticationFacade.getCurrentUser());

        boardRepository.delete(board);
    }

    /**
     * 내가 작성한 게시물 조회
     */
    @Transactional(readOnly = true)
    public Page<BoardListResponse> getWriteList(Pageable pageable) {
        User user = authenticationFacade.getCurrentUser();
        Page<Board> page = boardRepository.findAllByCreatedBy(user, pageable);

        return mapBoardsToResponse(page);
    }

    /**
     * 내가 저장한 게시물 조회
     */
    @Transactional(readOnly = true)
    public Page<BoardListResponse> getSaveList(Pageable pageable) {
        User user = authenticationFacade.getCurrentUser();
        Page<Board> page = boardRepository.findSavedBoards(user.getId(), pageable);

        return mapBoardsToResponse(page);
    }

    private Page<BoardListResponse> mapBoardsToResponse(Page<Board> boardPage) {
        // 컨텐츠(공통) 조회
        List<Long> contentIds = boardPage.getContent().stream()
                .map(b -> b.getContent().getId())
                .toList();

        // 댓글 조회
        Map<Long, Long> commentCountMap = contentIds.isEmpty()
                ? Map.of()
                : communityCommentsService.getContentsComments(contentIds);

        // 좋아요 조회
        Map<Long, Long> contentLikeMap = contentIds.isEmpty()
                ? Map.of()
                : contentService.getContentLikeCount(contentIds);

        // 설정 여부 확인
        Set<Long> commentedContentIds = new HashSet<>();
        Set<Long> likedContentIds = new HashSet<>();
        Set<Long> savedContentIds = new HashSet<>();

        // 좋아요/댓글/저장 여부
        Optional<User> user = authenticationFacade.getCurrentUserOptional();
        if (user.isPresent()) {
            commentedContentIds = communityCommentsService.getUserCommentedContentIds(user.get().getId(), contentIds);
            likedContentIds = contentService.getUserLikedContentIds(user.get().getId(), contentIds);
            savedContentIds = contentService.getUserSavedContentIds(user.get().getId(), contentIds);
        }

        Set<Long> finalCommentedContentIds = commentedContentIds;
        Set<Long> finalLikedContentIds = likedContentIds;
        Set<Long> finalSavedContentIds = savedContentIds;
        return boardPage.map(board -> {
                    Long contentId = board.getContent().getId();

                    return boardMapper.toBoardListResponse(
                            board,
                            commentCountMap.getOrDefault(contentId, 0L),
                            contentLikeMap.getOrDefault(contentId, 0L),
                            finalCommentedContentIds.contains(contentId),
                            finalLikedContentIds.contains(contentId),
                            finalSavedContentIds.contains(contentId),
                            Objects.equals(board.getCreatedBy(), user.orElse(null))
                    );
                }
        );
    }

}
