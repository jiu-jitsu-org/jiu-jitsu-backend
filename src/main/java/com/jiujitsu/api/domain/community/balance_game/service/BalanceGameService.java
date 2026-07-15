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
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class BalanceGameService {

    private final BalanceGameRepository balanceGameRepository;
    private final BalanceGameVoteRepository balanceGameVoteRepository;
    private final BalanceGameFactory balanceGameFactory;
    private final BalanceGameMapper balanceGameMapper;
    private final CommunityCommentsService communityCommentsService;
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
     */
    @Transactional(readOnly = true)
    public BalanceGameResponse getDetail(Long contentId) {
        BalanceGame game = balanceGameRepository.findByContentId(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.BALANCE_GAME_NOT_FOUND));
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
     */
    public BalanceGameResponse create(BalanceGameCreateRequest request) {
        Content content = balanceGameFactory.createContent();
        BalanceGame game = balanceGameFactory.createBalanceGame(
                content,
                request.optionAText(), request.optionAImageFileId(),
                request.optionBText(), request.optionBImageFileId(),
                request.endAt()
        );
        game = balanceGameRepository.save(game);

        return balanceGameMapper.toResponse(game, 0L, 0L, 0L, null, LocalDateTime.now());
    }

    // 득표수/내 투표/댓글수를 채워 응답으로 변환
    private BalanceGameResponse toResponseWithVotes(BalanceGame game) {
        Long balanceGameId = game.getId();

        long voteCountA = balanceGameVoteRepository.countByBalanceGameIdAndOption(balanceGameId, BalanceGameOption.A);
        long voteCountB = balanceGameVoteRepository.countByBalanceGameIdAndOption(balanceGameId, BalanceGameOption.B);

        // 비로그인 사용자는 myVote = null (프론트에서 결과 숨김 처리)
        BalanceGameOption myVote = authenticationFacade.getCurrentUserOptional()
                .flatMap(user -> balanceGameVoteRepository.findByBalanceGameIdAndCreatedBy(balanceGameId, user))
                .map(BalanceGameVote::getOption)
                .orElse(null);

        long commentCount = communityCommentsService.getCountComments(game.getContent().getId());

        return balanceGameMapper.toResponse(game, commentCount, voteCountA, voteCountB, myVote, LocalDateTime.now());
    }
}
