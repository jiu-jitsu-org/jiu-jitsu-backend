package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.dto.AdminBalanceGameListItem;
import com.jiujitsu.api.domain.community.balance_game.dto.AdminBalanceGameListRequest;
import com.jiujitsu.api.domain.community.balance_game.dto.AdminBalanceGameStatsResponse;
import com.jiujitsu.api.domain.community.balance_game.dto.AdminBalanceGameUpdateRequest;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameCreateRequest;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameStatus;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.file.ImageFile;
import com.jiujitsu.api.domain.community.balance_game.factory.BalanceGameFactory;
import com.jiujitsu.api.domain.community.balance_game.mapper.BalanceGameMapper;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameRepository;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameVoteRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.dto.ContentViewResponse;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
public class BalanceGameService {

    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);
    private static final Pageable FIRST = PageRequest.of(0, 1);

    private final BalanceGameRepository balanceGameRepository;
    private final BalanceGameVoteRepository balanceGameVoteRepository;
    private final BalanceGameFactory balanceGameFactory;
    private final BalanceGameMapper balanceGameMapper;
    private final CommunityCommentsService communityCommentsService;
    private final ContentService contentService;
    private final NoticeService noticeService;
    private final AuthenticationFacade authenticationFacade;

    /**
     * 커뮤니티 메인 - 현재 진행중인 밸런스 게임 조회
     * 진행중인 게임이 없으면 방금 마감된 게임을 결과로 보여 준다 (00:00~00:30 버퍼, 투표하지 않은 사람도 결과를 보게).
     * 결과는 다음 게임이 시작할 때까지(없으면 마감 + 30분) 보여 주고, 그 시각을 resultUntil 로 내려 앱이 그때 다시 조회한다.
     * 둘 다 없으면 null 을 반환한다. (메인 화면에서 미노출 처리)
     */
    @Transactional(readOnly = true)
    public BalanceGameResponse getCurrent() {
        return getCurrent(LocalDateTime.now());
    }

    // 게임 고르기와 응답(closed·started·serverTime)을 같은 now 로 판정한다.
    // 시각을 따로 읽으면 마감 직전에 고른 진행 중 게임이 응답에선 closed=true·resultUntil=null 로 나가 앱이 다시 조회할 시각을 잃는다
    BalanceGameResponse getCurrent(LocalDateTime now) {
        return findCurrent(now)
                .map(current -> toResponseWithVotes(current.game(), now, current.resultUntil()))
                .orElse(null);
    }

    // 지금 메인에 보일 게임: 진행 중 → 없으면 결과 노출 시간 안의 가장 최근 마감 게임 (resultUntil 포함)
    private Optional<CurrentGame> findCurrent(LocalDateTime now) {
        Optional<BalanceGame> open = balanceGameRepository.findOpen(now, FIRST).stream().findFirst();
        if (open.isPresent()) {
            return open.map(game -> new CurrentGame(game, null));
        }
        return balanceGameRepository.findClosed(now, FIRST).stream().findFirst()
                .map(closed -> new CurrentGame(closed, closed.resultUntil(
                        balanceGameRepository.findUpcoming(now, FIRST).stream().findFirst().orElse(null))))
                .filter(current -> now.isBefore(current.resultUntil()));
    }

    // 메인에 보일 게임과, 마감된 게임이면 결과를 보여 주는 끝 시각 (진행 중이면 null)
    private record CurrentGame(BalanceGame game, LocalDateTime resultUntil) {
    }

    /**
     * 밸런스 게임 상세 조회 (마감된 게임도 조회 가능 - 결과만 노출)
     *
     * 조회수는 여기서 올리지 않는다 (#249) — 웹이 화면을 맞추려고 같은 방문 안에서 여러 번 부른다. 조회수는 increaseView
     */
    public BalanceGameResponse getDetail(Long contentId) {
        BalanceGame game = balanceGameRepository.findByContentId(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.BALANCE_GAME_NOT_FOUND));

        // 상세를 열었으면 이 게임에 대한 내 알림(댓글 · 답글 · 좋아요 · 결과)은 읽음 (#246, 글 상세와 같은 기준)
        authenticationFacade.getCurrentUserOptional()
                .ifPresent(user -> noticeService.readContentNotices(user.getId(), contentId));

        return toResponseWithVotes(game);
    }

    /**
     * 밸런스 게임 조회수 올리기 ('봤음', #249) — 상세를 열 때 한 번
     * - 메인의 '오늘의 게임'(getCurrent)은 커뮤니티 진입 때마다 자동으로 불려 세지 않는다 (노출 수가 되어버림)
     * - 운영자가 등록해 작성자 개념이 없어 작성자 예외 없음
     * - 같은 사용자의 재진입도 방문마다 센다 (사용자당 1회 집계 아님, #124). 같은 방문 안에서 다시 읽는 상세 조회는 세지 않는다
     */
    public ContentViewResponse increaseView(Long contentId) {
        if (balanceGameRepository.findByContentId(contentId).isEmpty()) {
            throw new ErrorException(ErrorCode.BALANCE_GAME_NOT_FOUND);
        }
        return contentService.increaseViewCount(contentId, false);
    }

    /**
     * 끝난 판의 결과를 봤음 기록 (#290) — 홈의 '어제 결과 한 번'과 결과 알림 대상이 같은 기록을 쓴다
     * - 내가 투표한 끝난 판만 처음 본 시각을 남긴다 (다시 불러도 처음 시각 그대로)
     * - 투표하지 않은 판·아직 안 끝난 판은 남기지 않고 넘어간다 (웹이 구분하지 않고 부른다)
     * - 그 판의 내 결과 알림도 읽음 (#291) — 홈에서 본 뒤 종 아이콘 점이 남지 않게
     */
    public void markResultSeen(Long contentId) {
        User user = authenticationFacade.getCurrentUser();

        BalanceGame game = balanceGameRepository.findByContentId(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.BALANCE_GAME_NOT_FOUND));

        LocalDateTime now = LocalDateTime.now();
        if (!game.isClosed(now)) {
            return;
        }
        balanceGameVoteRepository.markResultSeen(game.getId(), user, now);
        // 순서 중요: 봤음 갱신이 발송 중인 투표 행을 기다린 뒤라야 방금 커밋된 결과 알림까지 읽음이 된다
        noticeService.readBalanceResultNotice(user.getId(), contentId);
    }

    /**
     * 밸런스 게임 투표/변경/취소 (토글)
     * - 미투표         → 등록
     * - 같은 선택지 재요청 → 취소
     * - 다른 선택지     → 변경
     * 투표 반영 후 최신 상태를 그대로 반환한다. (클라이언트 재조회 불필요)
     */
    public BalanceGameResponse vote(Long contentId, BalanceGameOption option) {
        User user = authenticationFacade.getCurrentUser();

        BalanceGame game = balanceGameRepository.findByContentId(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.BALANCE_GAME_NOT_FOUND));

        game.validateVotable(LocalDateTime.now());

        Optional<BalanceGameVote> existing =
                balanceGameVoteRepository.findByBalanceGameIdAndCreatedBy(game.getId(), user);

        if (existing.isPresent()) {
            BalanceGameVote vote = existing.get();
            if (vote.getOption() == option) {
                balanceGameVoteRepository.delete(vote);     // 같은 선택지 재요청 → 투표 취소
            } else {
                vote.changeOption(option);                  // 다른 선택지 → 변경
            }
        } else {
            balanceGameVoteRepository.save(
                    BalanceGameVote.builder().balanceGame(game).option(option).build());
        }

        return toResponseWithVotes(game);
    }

    /**
     * 내가 투표한 밸런스 게임 목록 (커뮤니티 활동 > 밸런스 게임 탭)
     * - 마감된 게임 포함, gameDate 최신순 (쿼리에 고정)
     * - 투표를 취소한 게임은 제외 (취소 = 투표 행 삭제)
     * 항목은 상세 조회와 같은 응답이며, 목록 조회라 조회수는 올리지 않는다.
     */
    @Transactional(readOnly = true)
    public Page<BalanceGameResponse> getParticipatedList(Pageable pageable) {
        User user = authenticationFacade.getCurrentUser();

        // 클라이언트가 sort 를 보내도 쿼리의 ORDER BY 에 덧붙지 않도록 정렬을 제거한다.
        Pageable unsorted = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<BalanceGameVote> votePage = balanceGameVoteRepository.findMyVotes(user, unsorted);

        // 빈 페이지면 IN () 집계 쿼리를 보내지 않는다. (범위 밖 페이지여도 totalElements 는 유지)
        if (votePage.isEmpty()) {
            return new PageImpl<>(List.of(), unsorted, votePage.getTotalElements());
        }

        List<Long> balanceGameIds = votePage.getContent().stream()
                .map(vote -> vote.getBalanceGame().getId())
                .toList();
        List<Long> contentIds = votePage.getContent().stream()
                .map(vote -> vote.getBalanceGame().getContent().getId())
                .toList();

        // 득표수: balanceGameId -> (선택지 -> 득표수)
        Map<Long, Map<BalanceGameOption, Long>> voteCountMap = new HashMap<>();
        for (Object[] row : balanceGameVoteRepository.countByBalanceGameIdsGroupByOption(balanceGameIds)) {
            voteCountMap.computeIfAbsent((Long) row[0], id -> new HashMap<>())
                    .put((BalanceGameOption) row[1], ((Number) row[2]).longValue());
        }

        Map<Long, Long> commentCountMap = communityCommentsService.getContentsComments(contentIds);
        Map<Long, Long> likeCountMap = contentService.getContentLikeCount(contentIds);
        Set<Long> commentedContentIds = communityCommentsService.getUserCommentedContentIds(user.getId(), contentIds);
        Set<Long> likedContentIds = contentService.getUserLikedContentIds(user.getId(), contentIds);
        Set<Long> noticeDisabledContentIds = noticeService.getNoticeDisabledContentIds(user.getId(), contentIds);

        LocalDateTime now = LocalDateTime.now();
        return votePage.map(vote -> {
            BalanceGame game = vote.getBalanceGame();
            Long contentId = game.getContent().getId();
            Map<BalanceGameOption, Long> voteCounts = voteCountMap.getOrDefault(game.getId(), Map.of());

            return balanceGameMapper.toResponse(game,
                    commentCountMap.getOrDefault(contentId, 0L),
                    likeCountMap.getOrDefault(contentId, 0L),
                    likedContentIds.contains(contentId),
                    commentedContentIds.contains(contentId),
                    !noticeDisabledContentIds.contains(contentId),
                    voteCounts.getOrDefault(BalanceGameOption.A, 0L),
                    voteCounts.getOrDefault(BalanceGameOption.B, 0L),
                    vote.getOption(),       // 내 투표 = 조회 기준 행의 선택지
                    vote.isResultSeen(),
                    now,
                    null);
        });
    }

    /**
     * [임시] 관리자 밸런스 게임 등록 (테스트 데이터용)
     * 추후 정식 관리자 등록 기능으로 대체 예정.
     *
     * endAt 미입력 시 당일 한국 시간 23:59:59 자동 설정. 자동 설정된 값이 이미 과거라면(심야 요청) 다음 날 23:59:59로 설정.
     * startAt 만 넣으면 endAt 은 시작일 23:59:59 (다음 날 게임을 미리 등록할 수 있게).
     * startAt 미입력 시 마감일 00:30 (00:00~00:30 은 전날 결과 버퍼). startAt 이 endAt 보다 앞이 아니면 BALANCE_GAME_START_AT_NOT_BEFORE_END_AT
     * gameDate(진행일)는 시작일 — 기본 일정에선 마감일과 같고, 시작을 넣어 날짜를 넘기는 게임은 시작한 날이 진행일
     */
    public BalanceGameResponse create(BalanceGameCreateRequest request) {
        LocalDateTime endAt = resolveEndAt(request.endAt(), request.startAt());
        LocalDateTime startAt = request.startAt() != null ? request.startAt() : BalanceGame.defaultStartAt(endAt.toLocalDate());
        // 마감을 직접 넣었는데 그날 00:30 보다 이르거나, 시작을 마감 뒤로 넣으면 열릴 수 없는 게임이 된다
        if (!startAt.isBefore(endAt)) {
            throw new ErrorException(ErrorCode.BALANCE_GAME_START_AT_NOT_BEFORE_END_AT);
        }

        Content content = balanceGameFactory.createContent();
        BalanceGame game = balanceGameFactory.createBalanceGame(
                content,
                request.title(),
                request.optionAText(), request.optionAImageFileId(),
                request.optionBText(), request.optionBImageFileId(),
                startAt,
                endAt,
                startAt.toLocalDate()   // 진행일 = 시작일 (하루 1회 정책, 기본 일정에선 마감일과 같음)
        );
        game = balanceGameRepository.save(game);

        return balanceGameMapper.toResponse(game, 0L, 0L, false, false, null, 0L, 0L, null, LocalDateTime.now());
    }

    /**
     * endAt이 null이면 시작일(startAt 미입력이면 당일) 23:59:59를 기본값으로 사용한다.
     * startAt 없이 정한 기본값이 이미 과거라면(심야 요청) 다음 날로 넘긴다.
     * 명시적으로 입력된 endAt, 또는 startAt 으로 정한 기본값이 과거이면 예외를 던진다.
     * (JVM 타임존이 Asia/Seoul로 고정되어 있으므로 별도 ZoneId 지정 불필요)
     */
    private LocalDateTime resolveEndAt(LocalDateTime endAt, LocalDateTime startAt) {
        LocalDateTime now = LocalDateTime.now();

        if (endAt == null && startAt != null) {
            endAt = startAt.toLocalDate().atTime(END_OF_DAY);
        }
        if (endAt != null) {
            if (!endAt.isAfter(now)) {
                throw new ErrorException(ErrorCode.BALANCE_GAME_END_AT_MUST_BE_FUTURE);
            }
            return endAt;
        }

        // 미입력: 당일 23:59:59, 이미 지났으면 다음 날
        LocalDate today = LocalDate.now();
        LocalDateTime defaultEndAt = today.atTime(END_OF_DAY);
        return defaultEndAt.isAfter(now) ? defaultEndAt : today.plusDays(1).atTime(END_OF_DAY);
    }

    // 득표수/내 투표/댓글수 + 좋아요 지표를 채워 응답으로 변환
    private BalanceGameResponse toResponseWithVotes(BalanceGame game) {
        return toResponseWithVotes(game, LocalDateTime.now(), null);
    }

    // ── 관리자 API ──────────────────────────────────────────────────

    /**
     * 관리자 밸런스 게임 목록 (날짜 범위·상태 필터, gameDate 최신순)
     */
    @Transactional(readOnly = true)
    public Page<AdminBalanceGameListItem> getAdminList(AdminBalanceGameListRequest req, int page, int size) {
        LocalDateTime now = LocalDateTime.now();
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "gameDate", "id"));
        Page<BalanceGame> gamePage = balanceGameRepository.findAll(buildAdminListSpec(req, now), pageable);

        if (gamePage.isEmpty()) {
            return gamePage.map(g -> toAdminListItem(g, 0L, now));
        }

        List<Long> gameIds = gamePage.getContent().stream().map(BalanceGame::getId).toList();

        Map<Long, Long> totalVoteMap = new HashMap<>();
        for (Object[] row : balanceGameVoteRepository.countByBalanceGameIdsGroupByOption(gameIds)) {
            Long gameId = (Long) row[0];
            Long count = ((Number) row[2]).longValue();
            totalVoteMap.merge(gameId, count, Long::sum);
        }

        return gamePage.map(g -> toAdminListItem(g, totalVoteMap.getOrDefault(g.getId(), 0L), now));
    }

    /**
     * 관리자 밸런스 게임 상세 조회
     */
    @Transactional(readOnly = true)
    public BalanceGameResponse getAdminDetail(Long id) {
        BalanceGame game = balanceGameRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BALANCE_GAME_NOT_FOUND));
        return toResponseWithVotes(game);
    }

    /**
     * 관리자 밸런스 게임 수정
     * startAt/endAt 미입력 시 기존 값 유지. 수정 후 최신 상태를 반환.
     */
    public BalanceGameResponse updateAdmin(Long id, AdminBalanceGameUpdateRequest request) {
        BalanceGame game = balanceGameRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BALANCE_GAME_NOT_FOUND));

        LocalDateTime startAt = request.startAt() != null ? request.startAt() : game.getStartAt();
        LocalDateTime endAt = request.endAt() != null ? request.endAt() : game.getEndAt();

        if (!startAt.isBefore(endAt)) {
            throw new ErrorException(ErrorCode.BALANCE_GAME_START_AT_NOT_BEFORE_END_AT);
        }

        ImageFile optionAImage = balanceGameFactory.resolveImage(request.optionAImageFileId());
        ImageFile optionBImage = balanceGameFactory.resolveImage(request.optionBImageFileId());

        game.update(request.optionAText(), optionAImage, request.optionBText(), optionBImage, startAt, endAt);

        return toResponseWithVotes(game);
    }

    /**
     * 관리자 밸런스 게임 삭제
     * 연결된 Content(댓글·좋아요·조회수)와 투표 기록도 함께 삭제된다.
     */
    public void deleteAdmin(Long id) {
        BalanceGame game = balanceGameRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BALANCE_GAME_NOT_FOUND));
        balanceGameRepository.delete(game);
    }

    /**
     * 관리자 밸런스 게임 결과 분석 (참여 수, 선택 비율, 댓글·조회 수)
     */
    @Transactional(readOnly = true)
    public AdminBalanceGameStatsResponse getAdminStats(Long id) {
        BalanceGame game = balanceGameRepository.findById(id)
                .orElseThrow(() -> new ErrorException(ErrorCode.BALANCE_GAME_NOT_FOUND));

        long voteCountA = balanceGameVoteRepository.countByBalanceGameIdAndOption(id, BalanceGameOption.A);
        long voteCountB = balanceGameVoteRepository.countByBalanceGameIdAndOption(id, BalanceGameOption.B);
        long total = voteCountA + voteCountB;

        double rateA = total > 0
                ? BigDecimal.valueOf((double) voteCountA / total * 100).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;
        double rateB = total > 0
                ? BigDecimal.valueOf((double) voteCountB / total * 100).setScale(2, RoundingMode.HALF_UP).doubleValue()
                : 0.0;

        long commentCount = communityCommentsService.getCountComments(game.getContent().getId());

        return new AdminBalanceGameStatsResponse(
                id,
                game.getContent().getId(),
                game.getGameDate(),
                game.getStartAt(),
                game.getEndAt(),
                voteCountA,
                voteCountB,
                total,
                rateA,
                rateB,
                commentCount,
                game.getContent().getViewCount()
        );
    }

    private Specification<BalanceGame> buildAdminListSpec(AdminBalanceGameListRequest req, LocalDateTime now) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (req.dateFrom() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("gameDate"), req.dateFrom()));
            }
            if (req.dateTo() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("gameDate"), req.dateTo()));
            }
            if (req.status() != null) {
                predicates.add(switch (req.status()) {
                    case UPCOMING -> cb.greaterThan(root.get("startAt"), now);
                    case ONGOING  -> cb.and(
                            cb.lessThanOrEqualTo(root.get("startAt"), now),
                            cb.greaterThanOrEqualTo(root.get("endAt"), now)
                    );
                    case CLOSED   -> cb.lessThan(root.get("endAt"), now);
                });
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private AdminBalanceGameListItem toAdminListItem(BalanceGame game, long totalVoteCount, LocalDateTime now) {
        BalanceGameStatus status;
        if (game.getStartAt().isAfter(now)) {
            status = BalanceGameStatus.UPCOMING;
        } else if (!game.getEndAt().isBefore(now)) {
            status = BalanceGameStatus.ONGOING;
        } else {
            status = BalanceGameStatus.CLOSED;
        }
        return new AdminBalanceGameListItem(
                game.getId(),
                game.getContent().getId(),
                game.getGameDate(),
                game.getStartAt(),
                game.getEndAt(),
                game.getOptionAText(),
                game.getOptionBText(),
                totalVoteCount,
                status
        );
    }

    // now: 응답의 serverTime·started·closed 기준 시각. resultUntil: 메인에서 마감된 게임을 결과로 보여 줄 때만 (그 외 null)
    private BalanceGameResponse toResponseWithVotes(BalanceGame game, LocalDateTime now, LocalDateTime resultUntil) {
        Long balanceGameId = game.getId();
        Long contentId = game.getContent().getId();
        List<Long> contentIds = List.of(contentId);

        long voteCountA = balanceGameVoteRepository.countByBalanceGameIdAndOption(balanceGameId, BalanceGameOption.A);
        long voteCountB = balanceGameVoteRepository.countByBalanceGameIdAndOption(balanceGameId, BalanceGameOption.B);

        Optional<User> currentUser = authenticationFacade.getCurrentUserOptional();

        Optional<BalanceGameVote> vote = currentUser
                .flatMap(user -> balanceGameVoteRepository.findByBalanceGameIdAndCreatedBy(balanceGameId, user));

        // 비로그인 사용자는 myVote = null (프론트에서 결과 숨김 처리)
        BalanceGameOption myVote = vote.map(BalanceGameVote::getOption).orElse(null);
        boolean resultSeen = vote.map(BalanceGameVote::isResultSeen).orElse(false);

        long commentCount = communityCommentsService.getCountComments(contentId);

        // 좋아요는 Content 공통 기능이라 게시글과 같은 집계를 그대로 사용한다.
        // 저장은 밸런스 게임 미지원 (ContentService.save 에서 차단) 이라 응답에도 내리지 않는다.
        long likeCount = contentService.getContentLikeCount(contentIds).getOrDefault(contentId, 0L);

        // 비로그인 사용자는 내 상태가 없으므로 false / null
        boolean isLiked = currentUser
                .map(user -> contentService.getUserLikedContentIds(user.getId(), contentIds).contains(contentId))
                .orElse(false);

        boolean isCommented = currentUser
                .map(user -> communityCommentsService.getUserCommentedContentIds(user.getId(), contentIds).contains(contentId))
                .orElse(false);

        Boolean noticeEnabled = currentUser
                .map(user -> noticeService.isContentNoticeEnabled(user.getId(), contentId))
                .orElse(null);

        return balanceGameMapper.toResponse(game, commentCount, likeCount, isLiked,
                isCommented, noticeEnabled, voteCountA, voteCountB, myVote, resultSeen, now, resultUntil);
    }
}
