package com.jiujitsu.api.domain.community.comment.service;

import com.jiujitsu.api.domain.community.comment.dto.CommentsListRequest;
import com.jiujitsu.api.domain.community.comment.dto.CommentsSortType;
import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsResponse;
import com.jiujitsu.api.domain.community.comment.dto.CommunityCommentsWriteRequest;
import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeRequest;
import com.jiujitsu.api.domain.community.comment.dto.like.CommentLikeResponse;
import com.jiujitsu.api.domain.community.comment.entity.CommentLike;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.event.CommentNoticeEvent;
import com.jiujitsu.api.domain.community.comment.factory.CommentFactory;
import com.jiujitsu.api.domain.community.comment.factory.CommentLikeFactory;
import com.jiujitsu.api.domain.community.comment.mapper.CommentLikeMapper;
import com.jiujitsu.api.domain.community.comment.mapper.CommentMapper;
import com.jiujitsu.api.domain.community.comment.repository.CommentLikeRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.repository.ContentRepository;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.service.ReportService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.domain.user.service.UserBlockService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.util.AuthenticationUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class CommunityCommentsService {

    private final CommentLikeRepository commentLikeRepository;
    private final CommunityCommentsRepository communityCommentsRepository;
    private final ContentRepository contentRepository;
    private final AuthenticationFacade authenticationFacade;
    private final UserBlockService userBlockService;
    private final ReportService reportService;
    private final ApplicationEventPublisher eventPublisher;
    private final CommentFactory commentFactory;
    private final CommentMapper commentMapper;
    private final CommentLikeFactory commentLikeFactory;
    private final CommentLikeMapper commentLikeMapper;


    private static final int INITIAL_CHILD_LIMIT = 3;
    private static final int REPLY_PAGE_SIZE = 10;

    /**
     * 댓글 목록 조회
     * 대댓글은 sortType 기준 3개만 미리보기로 포함한다. 나머지는 대댓글 추가 조회로 가져온다.
     */
    @Transactional(readOnly = true)
    public List<CommunityCommentsResponse> getComments(CommentsListRequest request) {
        Long contentId = request.id();
        CommentsSortType sortType = request.sortType();

        contentRepository.findById(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));

        Set<Long> blockedUserIds = new HashSet<>(userBlockService.getBlockedUserIds());
        Set<Long> reportedCommentIdSet = new HashSet<>(reportService.getReportedTargetIds(ReportType.COMMENT));

        List<CommunityComments> comments = communityCommentsRepository.findByContentIdFiltered(contentId);
        CommentViewContext viewContext = buildViewContext(comments, blockedUserIds, reportedCommentIdSet);

        Map<Long, List<CommunityComments>> childrenByParentId = comments.stream()
                .filter(c -> !isRootComment(c))
                .collect(Collectors.groupingBy(CommunityComments::getParentId, LinkedHashMap::new, Collectors.toList()));

        List<CommunityComments> roots = comments.stream()
                .filter(this::isRootComment)
                .toList();

        List<CommunityCommentsResponse> result = roots.stream()
                .map(root -> {
                    List<CommunityComments> children = sortComments(
                            childrenByParentId.getOrDefault(root.getId(), List.of()),
                            sortType
                    );
                    List<CommunityCommentsResponse> preview = children.stream()
                            .limit(INITIAL_CHILD_LIMIT)
                            .map(child -> toResponse(child, new ArrayList<>(), 0L, viewContext))
                            .toList();
                    return toResponse(root, new ArrayList<>(preview), (long) children.size(), viewContext);
                })
                .toList();

        return Objects.equals(sortType, CommentsSortType.CREATE_DESC)
                ? result.stream().sorted(Comparator.comparing(CommunityCommentsResponse::createdAt).reversed()).toList()
                : result.stream().sorted(Comparator.comparing(CommunityCommentsResponse::createdAt)).toList();
    }

    /**
     * 대댓글 추가 조회
     * 댓글 목록의 미리보기 3개 이후부터 10개씩 페이징한다. 정렬은 댓글 목록과 동일한 sortType을 따른다.
     */
    @Transactional(readOnly = true)
    public Slice<CommunityCommentsResponse> getReplies(Long commentId, int page, CommentsSortType sortType) {
        if (page < 0) {
            throw new ErrorException(ErrorCode.WRONG_PARAMETER);
        }

        communityCommentsRepository.findById(commentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMMENT_NOT_FOUND));

        Set<Long> blockedUserIds = new HashSet<>(userBlockService.getBlockedUserIds());
        Set<Long> reportedCommentIdSet = new HashSet<>(reportService.getReportedTargetIds(ReportType.COMMENT));

        Sort sort = toSort(sortType);
        long offset = INITIAL_CHILD_LIMIT + (long) page * REPLY_PAGE_SIZE;
        Pageable pageable = new OffsetLimitRequest(offset, REPLY_PAGE_SIZE + 1, sort);

        List<CommunityComments> fetched = communityCommentsRepository.findRepliesByParentId(commentId, pageable);
        boolean hasNext = fetched.size() > REPLY_PAGE_SIZE;
        List<CommunityComments> pageItems = hasNext ? fetched.subList(0, REPLY_PAGE_SIZE) : fetched;

        CommentViewContext viewContext = buildViewContext(pageItems, blockedUserIds, reportedCommentIdSet);
        List<CommunityCommentsResponse> content = pageItems.stream()
                .map(reply -> toResponse(reply, new ArrayList<>(), 0L, viewContext))
                .toList();

        return new SliceImpl<>(content, PageRequest.of(page, REPLY_PAGE_SIZE, sort), hasNext);
    }

    /**
     * 댓글 작성
     */
    public CommunityCommentsResponse createComment(CommunityCommentsWriteRequest request) {
        // 로그인 확인
        User user = authenticationFacade.getCurrentUser();

        // 컨텐츠 조회
        Content content = contentRepository.findById(request.contentId())
                .orElseThrow(() -> new ErrorException(ErrorCode.CONTENT_NOT_FOUND));

        // 대댓글일 경우 부모댓글 조회
        Long parentID = request.parentId() == null ? 0L : request.parentId();
        Optional<CommunityComments> parentComments = communityCommentsRepository.findById(parentID);


        // 댓글 entity 생성
        CommunityComments communityComments = commentFactory.createComments(content, parentID, request.body());
        communityCommentsRepository.save(communityComments);

        // 알림 설정 (커밋 후 FCM 발송 + 알림 저장)
        User boardWriter = content.getCreatedBy();

        // 알림1 - 게시글에 댓글 달렸을 때 게시글 작성자에게
        if (!Objects.equals(user.getId(), boardWriter.getId())) {
            eventPublisher.publishEvent(new CommentNoticeEvent(
                    boardWriter.getId(),
                    content.getId(),
                    FcmPushType.NEW_COMMENTS,
                    Map.of("type", FcmPushType.NEW_COMMENTS.getActionType().toString(),
                           "data", content.getId().toString())
            ));
        }

        // 알림2 - 댓글에 대댓글 달렸을 때 댓글 작성자에게
        if (parentComments.isPresent()) {
            if (!Objects.equals(user.getId(), parentComments.get().getCreatedBy().getId())) {
                eventPublisher.publishEvent(new CommentNoticeEvent(
                        parentComments.get().getCreatedBy().getId(),
                        content.getId(),
                        FcmPushType.NEW_CHILD_COMMENTS,
                        Map.of("type", FcmPushType.NEW_CHILD_COMMENTS.getActionType().toString(),
                               "data", content.getId().toString())
                ));
            }
        }

        return commentMapper.toCommunityCommentsResponse(communityComments, new ArrayList<>());
    }

    /**
     * 댓글 좋아요 등록
     */
    public CommentLikeResponse createCommentLike(CommentLikeRequest request) {
        // 로그인 유저 정보 조회
        User user = authenticationFacade.getCurrentUser();

        // 댓글 조회
        CommunityComments comment = communityCommentsRepository.findById(request.commentId())
                .orElseThrow(() -> new ErrorException(ErrorCode.COMMENT_NOT_FOUND));

        // 기존 좋아요 조회
        CommentLike newLike = null;
        Optional<CommentLike> existLike = commentLikeRepository.findByCommentIdAndCreatedBy(comment.getId(), user);

        if (existLike.isPresent()) {
            // 좋아요 취소
            commentLikeRepository.delete(existLike.get());
        } else {
            // 좋아요 등록
            newLike = commentLikeFactory.createCommentLike(comment);
            commentLikeRepository.save(newLike);

            if (!Objects.equals(user.getId(), comment.getCreatedBy().getId())) {
                eventPublisher.publishEvent(new CommentNoticeEvent(
                        comment.getCreatedBy().getId(),
                        comment.getContent().getId(),
                        FcmPushType.COMMENTS_LIKE,
                        Map.of("type", FcmPushType.COMMENTS_LIKE.getActionType().toString(),
                               "data", comment.getId().toString())
                ));
            }
        }

        long likeCount = commentLikeRepository.countGroupByCommentIds(List.of(comment.getId()))
                .stream()
                .findFirst()
                .map(row -> ((Number) row[1]).longValue())
                .orElse(0L);

        return commentLikeMapper.toCommentLikeResponse(comment, newLike, likeCount);
    }

    /**
     * 댓글 삭제
     * 대댓글이 있으면 soft-delete (deletedAt 설정), 없으면 hard-delete
     */
    public void deleteComment(Long commentId) {
        CommunityComments comment = communityCommentsRepository.findById(commentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMMENT_NOT_FOUND));

        // 수정 권한 체크
        comment.validateOwner(authenticationFacade.getCurrentUser());

        if (communityCommentsRepository.existsByParentId(commentId)) {
            comment.softDelete();
        } else {
            communityCommentsRepository.delete(comment);
        }
    }

    /**
     * 게시글 단건 > 댓글 카운트 조회
     */
    @Transactional(readOnly = true)
    public long getCountComments(Long contentId) {
        return communityCommentsRepository
                .countVisibleRootComments(contentId);
    }

    /**
     * 게시글 목록 > 댓글 카운트 조회
     */
    @Transactional(readOnly = true)
    public Map<Long, Long> getContentsComments(List<Long> contentIds) {
        return communityCommentsRepository.countTopLevelCommentsByContentIds(contentIds).stream()
                .collect(Collectors.toMap(row -> (Long) row[0], row -> ((Number) row[1]).longValue()));
    }

    /**
     * 게시글 - 댓글id 조회
     */
    @Transactional(readOnly = true)
    public Set<Long> getUserCommentedContentIds(Long userId, List<Long> contentIds) {
        return communityCommentsRepository.findUserCommentedContentIds(userId, contentIds);
    }

    private boolean isRootComment(CommunityComments comment) {
        return comment.getParentId() == null || Objects.equals(comment.getParentId(), 0L);
    }

    private boolean isCreateDesc(CommentsSortType sortType) {
        return Objects.equals(sortType, CommentsSortType.CREATE_DESC);
    }

    private Sort toSort(CommentsSortType sortType) {
        Sort.Direction direction = isCreateDesc(sortType) ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, "createdAt").and(Sort.by(direction, "id"));
    }

    private List<CommunityComments> sortComments(List<CommunityComments> comments, CommentsSortType sortType) {
        Comparator<CommunityComments> comparator = Comparator
                .comparing(CommunityComments::getCreatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(CommunityComments::getId);
        if (isCreateDesc(sortType)) {
            comparator = comparator.reversed();
        }
        return comments.stream().sorted(comparator).toList();
    }

    private CommentViewContext buildViewContext(List<CommunityComments> comments,
                                                Set<Long> blockedUserIds,
                                                Set<Long> reportedCommentIdSet) {
        List<Long> commentIds = comments.stream().map(CommunityComments::getId).toList();

        Map<Long, Long> likeCountMap = commentIds.isEmpty()
                ? Map.of()
                : commentLikeRepository.countGroupByCommentIds(commentIds).stream()
                .collect(Collectors.toMap(
                        row -> (Long) row[0],
                        row -> ((Number) row[1]).longValue()
                ));

        Set<Long> likedCommentIds = commentIds.isEmpty()
                ? Set.of()
                : AuthenticationUtil.getCurrentUserId()
                .map(userId -> new HashSet<>(commentLikeRepository.findLikedCommentIds(commentIds, userId)))
                .orElseGet(HashSet::new);

        User user = authenticationFacade.getCurrentUserOptional().orElse(null);
        Set<Long> repliedParentIds = (user == null || commentIds.isEmpty())
                ? Set.of()
                : communityCommentsRepository.findRepliedParentIds(user.getId(), commentIds);

        return new CommentViewContext(likeCountMap, likedCommentIds, user, blockedUserIds, reportedCommentIdSet, repliedParentIds);
    }

    private CommunityCommentsResponse toResponse(CommunityComments comment,
                                                 List<CommunityCommentsResponse> childrenList,
                                                 Long childCount,
                                                 CommentViewContext viewContext) {
        return commentMapper.toCommunityCommentsResponse(
                comment,
                childrenList,
                viewContext.likeCountMap().getOrDefault(comment.getId(), 0L),
                viewContext.likedCommentIds().contains(comment.getId()),
                Objects.equals(comment.getCreatedBy(), viewContext.user()),
                viewContext.reportedCommentIdSet().contains(comment.getId()),
                comment.getCreatedBy() != null && viewContext.blockedUserIds().contains(comment.getCreatedBy().getId()),
                viewContext.repliedParentIds().contains(comment.getId()),
                childCount
        );
    }

    private record CommentViewContext(
            Map<Long, Long> likeCountMap,
            Set<Long> likedCommentIds,
            User user,
            Set<Long> blockedUserIds,
            Set<Long> reportedCommentIdSet,
            Set<Long> repliedParentIds
    ) {
    }

    private record OffsetLimitRequest(long offset, int size, Sort sort) implements Pageable {
        @Override
        public int getPageNumber() {
            return 0;
        }

        @Override
        public int getPageSize() {
            return size;
        }

        @Override
        public long getOffset() {
            return offset;
        }

        @Override
        public Sort getSort() {
            return sort;
        }

        @Override
        public Pageable next() {
            return new OffsetLimitRequest(offset + size, size, sort);
        }

        @Override
        public Pageable previousOrFirst() {
            return this;
        }

        @Override
        public Pageable first() {
            return this;
        }

        @Override
        public Pageable withPage(int pageNumber) {
            return this;
        }

        @Override
        public boolean hasPrevious() {
            return false;
        }
    }

}
