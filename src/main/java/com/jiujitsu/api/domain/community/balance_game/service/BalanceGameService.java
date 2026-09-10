package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameCreateRequest;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameOption;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGameVote;
import com.jiujitsu.api.domain.community.balance_game.factory.BalanceGameFactory;
import com.jiujitsu.api.domain.community.balance_game.mapper.BalanceGameMapper;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameRepository;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameVoteRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.content.service.ContentService;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BalanceGameService {

    private static final LocalTime END_OF_DAY = LocalTime.of(23, 59, 59);

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
     * 진행중인 게임이 없으면 null 을 반환한다. (메인 화면에서 미노출 처리)
     */
    @Transactional(readOnly = true)
    public BalanceGameResponse getCurrent() {
        return balanceGameRepository.findTopByEndAtAfterOrderByEndAtAsc(LocalDateTime.now())
                .map(this::toResponseWithVotes)
                .orElse(null);
    }

    /**
     * 밸런스 게임 상세 조회 (마감된 게임도 조회 가능 - 결과만 노출)
     *
     * 조회수는 상세 진입 시에만 증가시킨다.
     * 메인의 '오늘의 게임'(getCurrent)은 사용자가 의도해서 연 화면이 아니라
     * 커뮤니티 진입 때마다 자동 호출되므로 카운트하면 노출 수가 되어버린다.
     * 밸런스 게임은 운영자가 등록해 작성자 개념이 없으므로 게시글과 달리 작성자 예외 처리는 두지 않는다.
     * 같은 사용자의 재진입도 그대로 누적한다. (사용자당 1회 집계 아님 - 의도된 동작, #124 에서 확정)
     */
    public BalanceGameResponse getDetail(Long contentId) {
        BalanceGame game = balanceGameRepository.findByContentId(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.BALANCE_GAME_NOT_FOUND));

        game.getContent().incrementViewCount();

        return toResponseWithVotes(game);
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

        if (game.getEndAt().isBefore(LocalDateTime.now())) {
            throw new ErrorException(ErrorCode.BALANCE_GAME_CLOSED);
        }

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
     * [임시] 관리자 밸런스 게임 등록 (테스트 데이터용)
     * 추후 정식 관리자 등록 기능으로 대체 예정.
     *
     * endAt 미입력 시 당일 한국 시간 23:59:59 자동 설정.
     * 자동 설정된 값이 이미 과거라면(심야 요청) 다음 날 23:59:59로 설정.
     * gameDate(진행일)는 확정된 endAt 의 날짜를 그대로 고정 저장한다.
     */
    public BalanceGameResponse create(BalanceGameCreateRequest request) {
        LocalDateTime endAt = resolveEndAt(request.endAt());

        Content content = balanceGameFactory.createContent();
        BalanceGame game = balanceGameFactory.createBalanceGame(
                content,
                request.optionAText(), request.optionAImageFileId(),
                request.optionBText(), request.optionBImageFileId(),
                endAt,
                endAt.toLocalDate()     // 진행일 = 마감일의 날짜 (하루 1회 정책)
        );
        game = balanceGameRepository.save(game);

        return balanceGameMapper.toResponse(game, 0L, 0L, false, false, null, 0L, 0L, null, LocalDateTime.now());
    }

    /**
     * endAt이 null이면 당일 23:59:59를 기본값으로 사용한다.
     * 기본값이 이미 과거라면(심야 요청) 다음 날로 넘긴다.
     * 명시적으로 입력된 endAt이 과거이면 예외를 던진다.
     * (JVM 타임존이 Asia/Seoul로 고정되어 있으므로 별도 ZoneId 지정 불필요)
     */
    private LocalDateTime resolveEndAt(LocalDateTime endAt) {
        LocalDateTime now = LocalDateTime.now();

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
        Long balanceGameId = game.getId();
        Long contentId = game.getContent().getId();
        List<Long> contentIds = List.of(contentId);

        long voteCountA = balanceGameVoteRepository.countByBalanceGameIdAndOption(balanceGameId, BalanceGameOption.A);
        long voteCountB = balanceGameVoteRepository.countByBalanceGameIdAndOption(balanceGameId, BalanceGameOption.B);

        Optional<User> currentUser = authenticationFacade.getCurrentUserOptional();

        // 비로그인 사용자는 myVote = null (프론트에서 결과 숨김 처리)
        BalanceGameOption myVote = currentUser
                .flatMap(user -> balanceGameVoteRepository.findByBalanceGameIdAndCreatedBy(balanceGameId, user))
                .map(BalanceGameVote::getOption)
                .orElse(null);

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
                isCommented, noticeEnabled, voteCountA, voteCountB, myVote, LocalDateTime.now());
    }
}
