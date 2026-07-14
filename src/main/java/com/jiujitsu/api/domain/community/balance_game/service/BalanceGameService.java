package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameCreateRequest;
import com.jiujitsu.api.domain.community.balance_game.dto.BalanceGameResponse;
import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.factory.BalanceGameFactory;
import com.jiujitsu.api.domain.community.balance_game.mapper.BalanceGameMapper;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameRepository;
import com.jiujitsu.api.domain.community.comment.service.CommunityCommentsService;
import com.jiujitsu.api.domain.community.content.entity.Content;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional
public class BalanceGameService {

    private final BalanceGameRepository balanceGameRepository;
    private final BalanceGameFactory balanceGameFactory;
    private final BalanceGameMapper balanceGameMapper;
    private final CommunityCommentsService communityCommentsService;

    /**
     * 커뮤니티 메인 - 현재 진행중인 밸런스 게임 조회
     * 진행중인 게임이 없으면 null 을 반환한다. (메인 화면에서 미노출 처리)
     */
    @Transactional(readOnly = true)
    public BalanceGameResponse getCurrent() {
        return balanceGameRepository.findTopByEndAtAfterOrderByEndAtAsc(LocalDateTime.now())
                .map(game -> balanceGameMapper.toResponse(
                        game,
                        communityCommentsService.getCountComments(game.getContent().getId())
                ))
                .orElse(null);
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

        return balanceGameMapper.toResponse(game, 0L);
    }
}
