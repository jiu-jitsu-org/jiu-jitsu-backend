package com.jiujitsu.api.domain.community.board.service;

import com.jiujitsu.api.domain.community.board.dto.*;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.entity.BoardCategory;
import com.jiujitsu.api.domain.community.board.entity.BoardListType;
import com.jiujitsu.api.domain.community.board.factory.BoardFactory;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.community.board.mapper.BoardMapper;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.community.profile.entity.CommunityProfile;
import com.jiujitsu.api.domain.community.profile.service.CommunityProfileService;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.competition.dto.CompetitionListItem;
import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.service.CompetitionQueryService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Transactional
public class BoardService {

    private final AuthenticationFacade authenticationFacade;
    private final UserRepository userRepository;
    private final BoardRepository boardRepository;
    private final BoardFactory boardFactory;
    private final BoardMapper boardMapper;
    private final BoardCategoryService boardCategoryService;
    private final CommunityCommentsService communityCommentsService;
    private final ContentService contentService;
    private final UserBlockService userBlockService;
    private final NoticeService noticeService;
    private final ReportService reportService;
    private final BoardHideService boardHideService;
    private final CommunityProfileService communityProfileService;
    private final CompetitionQueryService competitionQueryService;

    private static final int COMPETITION_STORY_LIMIT = 5;
    private static final int COMPETITION_STORY_REACTION_LIMIT = 2;

    /**
     * 게시물 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<BoardListResponse> getList(BoardListRequest boardListRequest, Pageable pageable) {
        BoardListType boardListType = boardListRequest.boardListType();
        FeedExclusions excluded = getFeedExclusions();

        Page<Board> page = switch (boardListType) {
            case CATEGORY -> boardRepository.findAllByCategoryFiltered(
                    boardListRequest.categoryId(), excluded.authorIds(), excluded.contentIds(), pageable);
            case SEARCH -> {
                String keyword = StringUtils.trimToEmpty(boardListRequest.searchKeyword()).toUpperCase(Locale.ROOT);
                yield boardRepository.findByTitleBodyKeywordFiltered(keyword, excluded.authorIds(), excluded.contentIds(), pageable);
            }
            default -> boardRepository.findAllFiltered(excluded.authorIds(), excluded.contentIds(), pageable);
        };

        return mapBoardsToResponse(page);
    }

    /**
     * 대회 이야기 (대회 상세, 비로그인 가능) — 이 대회를 고른 글 최대 5개
     * 반응(댓글 + 좋아요)이 있는 글 중 많은 순 2개를 먼저, 나머지는 최신순으로 채운다 (겹치지 않게). 반응이 없으면 최신순
     * 피드와 같은 글이 빠진다 (숨김·차단 작성자·내가 신고·내가 숨김). 없거나 미공개 대회면 COMPETITION_NOT_FOUND
     */
    @Transactional(readOnly = true)
    public List<BoardListResponse> getCompetitionStories(Long competitionId) {
        competitionQueryService.getPublished(competitionId);
        FeedExclusions excluded = getFeedExclusions();

        List<Board> stories = new ArrayList<>(boardRepository.findCompetitionStoriesByReaction(
                competitionId, excluded.authorIds(), excluded.contentIds(), PageRequest.of(0, COMPETITION_STORY_REACTION_LIMIT)));
        // 반응 상위 글이 최신 글에도 들 수 있어 최대 개수만큼 가져와 겹치는 글을 건너뛴다
        Set<Long> pickedIds = stories.stream().map(Board::getId).collect(Collectors.toSet());
        boardRepository.findCompetitionStoriesLatest(
                        competitionId, excluded.authorIds(), excluded.contentIds(), PageRequest.of(0, COMPETITION_STORY_LIMIT))
                .stream()
                .filter(board -> !pickedIds.contains(board.getId()))
                .limit(COMPETITION_STORY_LIMIT - stories.size())
                .forEach(stories::add);

        return mapBoardsToResponses(stories);
    }

    /**
     * 게시물 상세 조회
     */
    public BoardResponse getById(Long contentId) {
        // 게시글 조회
        Board board = boardRepository.findByContent_Id(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        // 전체 숨김 처리된 게시물
        if (board.isHidden()) {
            throw new ErrorException(ErrorCode.BOARD_NOT_FOUND);
        }

        // 신고한 게시물은 신고자에게 숨김
        authenticationFacade.getCurrentUserOptional().ifPresent(user -> {
            if (reportService.hasReported(user, ReportType.BOARD, contentId)) {
                throw new ErrorException(ErrorCode.BOARD_NOT_FOUND);
            }
        });

        // 조회수 증가 (작성자는 최초 1회만 카운트)
        Optional<User> currentUser = authenticationFacade.getCurrentUserOptional();
        boolean isAuthor = Objects.equals(board.getCreatedBy(), currentUser.orElse(null));
        if (isAuthor) {
            board.getContent().incrementViewCountForAuthor();
        } else {
            board.getContent().incrementViewCount();
        }

        // 댓글 수 조회
        long commentCount = communityCommentsService.getCountComments(contentId);

        // 좋아요 수 조회
        long likeCount = contentService.getContentLikeCount(List.of(contentId))
                .getOrDefault(contentId, 0L);

        // 저장 수 조회
        long saveCount = contentService.getContentSaveCount(List.of(contentId))
                .getOrDefault(contentId, 0L);

        // 설정 여부 확인
        Set<Long> commentedContentIds = new HashSet<>();
        Set<Long> likedContentIds = new HashSet<>();
        Set<Long> savedContentIds = new HashSet<>();

        Boolean noticeEnabled = null;
        if (currentUser.isPresent()) {
            commentedContentIds = communityCommentsService.getUserCommentedContentIds(currentUser.get().getId(), List.of(contentId));
            likedContentIds = contentService.getUserLikedContentIds(currentUser.get().getId(), List.of(contentId));
            savedContentIds = contentService.getUserSavedContentIds(currentUser.get().getId(), List.of(contentId));
            noticeEnabled = noticeService.isContentNoticeEnabled(currentUser.get().getId(), contentId);
        }

        // 작성자 프로필 조회
        CommunityProfile authorProfile = getAuthorProfile(board);

        // 연결된 대회 카드
        CompetitionListItem competition = getCompetitionCard(board);

        // dto 생성
        return boardMapper.toResponse(
                board,
                authorProfile,
                commentCount,
                likeCount,
                saveCount,
                commentedContentIds.contains(contentId),
                likedContentIds.contains(contentId),
                savedContentIds.contains(contentId),
                noticeEnabled,
                isAuthor,
                competition
                );
    }

    /**
     * 게시물 작성
     */
    public BoardResponse create(BoardCreateRequest request) {
        // 로그인 유저 조회
        User user = authenticationFacade.getCurrentUser();

        // 등록 카테고리 조회
        BoardCategory category = boardCategoryService.findActiveCategory(request.categoryId());

        // 연결 대회 조회 (선택, 공개 대회만)
        Competition competition = request.competitionId() == null ? null : competitionQueryService.getPublished(request.competitionId());

        // content 생성
        Content content = boardFactory.createContent(request.imageFileIdListOrEmpty());

        // Board 생성
        Board board = boardFactory.createBoard(category, content, competition, request.title(), request.body(), request.tagsOrEmpty());
        board = boardRepository.save(board);

        // dto 생성
        return boardMapper.toResponse(board, user, communityProfileService.findProfile(user).orElse(null), getCompetitionCard(board));
    }

    /**
     * 게시물 수정
     */
    public BoardResponse update(Long contentId, BoardUpdateRequest request) {
        // 게시물 조회
        Board board = boardRepository.findByContent_Id(contentId)
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

        // 기존 이미지 모두 제거 후 재생성
        Content content = board.getContent();
        List<ImageFile> oldImages = new ArrayList<>(content.getImageFiles());
        content.getImageFiles().clear();
        List<ImageFile> newImages = boardFactory.createImageFiles(request.getImageFileIdList());
        content.addImageFiles(newImages);

        // 목록에서 빠진 이미지를 TEMP로 복귀 → TTL 배치가 수거
        Set<Long> newImageIds = newImages.stream().map(ImageFile::getId).collect(Collectors.toSet());
        oldImages.stream()
                .filter(img -> !newImageIds.contains(img.getId()))
                .forEach(ImageFile::deactivate);

        if (request.getTags() != null) {
            boardFactory.replaceTags(board, request.getTags());
        }

        // 대회 연결은 보낸 값으로 교체 (null = 해제). 지금 연결과 같으면 그대로 — 연결 뒤 미공개로 바뀐 대회도 유지
        if (!Objects.equals(request.getCompetitionId(), board.getCompetitionId())) {
            board.changeCompetition(request.getCompetitionId() == null
                    ? null : competitionQueryService.getPublished(request.getCompetitionId()));
        }

        // 이미지·태그만 바꾼 경우 Board 엔티티가 dirty 상태가 되지 않아
        // @LastModifiedDate 가 갱신되지 않으므로, 항상 touch() 로 dirty 상태를 보장한다.
        board.touch();

        // 수정여부 업데이트를 위한 flush 강제호출
        boardRepository.flush();

        // dto 생성
        return boardMapper.toResponse(board, board.getCreatedBy(), getAuthorProfile(board), getCompetitionCard(board));
    }

    /**
     * 게시물 삭제
     */
    public void delete(Long contentId) {
        Board board = boardRepository.findByContent_Id(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));

        // 권한 체크
        board.validateOwner(authenticationFacade.getCurrentUser());

        // 연결된 이미지를 TEMP로 복귀 → TTL 배치가 수거
        board.getContent().getImageFiles().forEach(ImageFile::deactivate);

        boardRepository.delete(board);
    }

    /**
     * 내가 작성한 게시물 조회
     */
    @Transactional(readOnly = true)
    public Page<BoardListResponse> getWriteList(Pageable pageable) {
        User user = authenticationFacade.getCurrentUser();
        Page<Board> page = boardRepository.findAllByCreatedByAndNotHidden(user, pageable);

        return mapBoardsToResponse(page);
    }

    /**
     * 특정 사용자가 쓴 글 목록 (비로그인 가능)
     * - 차단 관계이면 404
     * - 신고/숨김 콘텐츠 제외 (뷰어 기준)
     */
    @Transactional(readOnly = true)
    public Page<BoardListResponse> getUserBoardList(Long userId, Pageable pageable) {
        userRepository.findById(userId)
                .orElseThrow(() -> new ErrorException(ErrorCode.USER_NOT_FOUND));

        authenticationFacade.getCurrentUserOptional().ifPresent(viewer -> {
            if (userBlockService.isBlockedBetween(viewer.getId(), userId)) {
                throw new ErrorException(ErrorCode.USER_NOT_FOUND);
            }
        });

        List<Long> reportedContentIds = reportService.getReportedTargetIds(ReportType.BOARD);
        List<Long> hiddenContentIds = boardHideService.getHiddenContentIds();
        List<Long> excludedContentIds = Stream.concat(reportedContentIds.stream(), hiddenContentIds.stream())
                .distinct().toList();
        List<Long> excludedContentIdsSentinel = excludedContentIds.isEmpty() ? List.of(-1L) : excludedContentIds;

        Page<Board> page = boardRepository.findAllByUserFiltered(userId, excludedContentIdsSentinel, pageable);
        return mapBoardsToResponse(page);
    }

    /**
     * 내가 저장한 게시물 조회
     */
    @Transactional(readOnly = true)
    public Page<BoardListResponse> getSaveList(Pageable pageable) {
        User user = authenticationFacade.getCurrentUser();
        Page<Board> page = boardRepository.findSavedBoardsNotHidden(user.getId(), pageable);

        return mapBoardsToResponse(page);
    }

    private Page<BoardListResponse> mapBoardsToResponse(Page<Board> boardPage) {
        return new PageImpl<>(mapBoardsToResponses(boardPage.getContent()), boardPage.getPageable(), boardPage.getTotalElements());
    }

    private List<BoardListResponse> mapBoardsToResponses(List<Board> boards) {
        // 컨텐츠(공통) 조회
        List<Long> contentIds = boards.stream()
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

        // 저장 조회
        Map<Long, Long> contentSaveMap = contentIds.isEmpty()
                ? Map.of()
                : contentService.getContentSaveCount(contentIds);

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

        // 작성자 프로필 일괄 조회
        Map<Long, CommunityProfile> authorProfileMap = communityProfileService.getProfilesByUserIds(
                boards.stream()
                        .map(Board::getCreatedBy)
                        .filter(Objects::nonNull)
                        .map(User::getId)
                        .collect(Collectors.toSet())
        );

        // 연결된 대회 카드 일괄 조회 (미공개 대회는 카드 없음)
        Map<Long, CompetitionListItem> competitionCards = competitionQueryService.getCards(
                boards.stream()
                        .map(Board::getCompetitionId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet())
        );

        Set<Long> finalCommentedContentIds = commentedContentIds;
        Set<Long> finalLikedContentIds = likedContentIds;
        Set<Long> finalSavedContentIds = savedContentIds;
        return boards.stream().map(board -> {
                    Long contentId = board.getContent().getId();

                    return boardMapper.toBoardListResponse(
                            board,
                            board.getCreatedBy() == null ? null : authorProfileMap.get(board.getCreatedBy().getId()),
                            commentCountMap.getOrDefault(contentId, 0L),
                            contentLikeMap.getOrDefault(contentId, 0L),
                            contentSaveMap.getOrDefault(contentId, 0L),
                            finalCommentedContentIds.contains(contentId),
                            finalLikedContentIds.contains(contentId),
                            finalSavedContentIds.contains(contentId),
                            Objects.equals(board.getCreatedBy(), user.orElse(null)),
                            board.getCompetitionId() == null ? null : competitionCards.get(board.getCompetitionId())
                    );
                }
        ).toList();
    }

    // 연결된 대회 카드 (연결 없거나 미공개면 null)
    private CompetitionListItem getCompetitionCard(Board board) {
        Long competitionId = board.getCompetitionId();
        return competitionId == null ? null : competitionQueryService.getCards(List.of(competitionId)).get(competitionId);
    }

    /**
     * 피드에서 빼는 글: 내가 차단한 작성자, 내가 신고한 글, 내가 숨긴 글 (비로그인은 없음)
     * 빈 리스트일 때 IN 절 오류 방지용 sentinel(-1)
     */
    private FeedExclusions getFeedExclusions() {
        List<Long> blockedUserIds = userBlockService.getBlockedUserIds();
        List<Long> reportedContentIds = reportService.getReportedTargetIds(ReportType.BOARD);
        List<Long> hiddenContentIds = boardHideService.getHiddenContentIds();

        List<Long> excludedAuthorIds = blockedUserIds.isEmpty() ? List.of(-1L) : blockedUserIds;
        List<Long> mergedExcludedContentIds = Stream.concat(reportedContentIds.stream(), hiddenContentIds.stream())
                .distinct().toList();
        List<Long> excludedContentIds = mergedExcludedContentIds.isEmpty() ? List.of(-1L) : mergedExcludedContentIds;
        return new FeedExclusions(excludedAuthorIds, excludedContentIds);
    }

    private record FeedExclusions(List<Long> authorIds, List<Long> contentIds) {
    }

    // 작성자 커뮤니티 프로필 (미작성 시 null)
    private CommunityProfile getAuthorProfile(Board board) {
        return communityProfileService.findProfile(board.getCreatedBy()).orElse(null);
    }
}
