package com.jiujitsu.api.domain.community.balance_game.service;

import com.jiujitsu.api.domain.community.balance_game.entity.BalanceGame;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameRepository;
import com.jiujitsu.api.domain.community.balance_game.repository.BalanceGameVoteRepository;
import com.jiujitsu.api.domain.community.content.entity.ContentType;
import com.jiujitsu.api.domain.notice.service.NoticeService;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
import com.jiujitsu.api.global.properties.BalanceResultNoticeProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 끝난 밸런스 게임의 결과 알림 (#291). BalanceResultNoticeRunService 가 부른다 (매일 낮·관리자 재실행)
 * 받는 사람: 투표했고 결과를 안 본 사람 (탈퇴했거나 그 판의 알림 종을 끈 사람 제외)
 * 결과 알림 설정을 끈 사람은 알림함에만 남는다 — 푸시 발송 직전에 거른다 (FcmPushEventListener, 댓글 알림과 같음)
 */
@Service
@RequiredArgsConstructor
public class BalanceResultNoticeService {

    private final BalanceGameRepository balanceGameRepository;
    private final BalanceGameVoteRepository balanceGameVoteRepository;
    private final NoticeService noticeService;
    private final FcmPushEventPublisher fcmPushEventPublisher;
    private final BalanceResultNoticeProperties properties;
    private final Clock clock;

    /**
     * date 에 마감됐고 총 투표가 기준 이상인 판 목록
     * 진행일(gameDate)이 아니라 마감 시각의 날짜로 본다 — 날짜를 넘겨 진행한 판은 진행일이 더 이르다
     */
    @Transactional(readOnly = true)
    public List<DueNotice> findDue(LocalDate date) {
        return balanceGameRepository.findClosedBetweenWithMinVotes(
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay(), properties.minVotes()).stream()
                .map(DueNotice::of)
                .toList();
    }

    /**
     * 판 하나의 결과 알림을 받을 사람에게 보낸다. 투표 행의 보낸 기록을 남긴 사람에게만 보내 다시·동시에 돌아도 한 번만 간다
     * 판마다 트랜잭션을 따로 둬서 한 판이 실패해도 나머지는 보낸다 (실패한 판은 기록·알림이 함께 롤백, 푸시는 커밋 뒤 발송이라 안 나감)
     * @return 보낸 사람 수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int send(DueNotice due) {
        Map<String, String> data = Map.of(
                "type", ContentType.BALANCE.getPushActionType().name(),
                "data", String.valueOf(due.contentId()));
        LocalDateTime now = LocalDateTime.now(clock);
        int sent = 0;
        for (BalanceGameVoteRepository.ResultNoticeTarget target : balanceGameVoteRepository.findResultNoticeTargets(due.balanceGameId())) {
            if (balanceGameVoteRepository.markResultNoticed(target.getVoteId(), now) == 0) {
                continue;   // 그 사이 다른 실행이 보냈거나 결과를 봄
            }
            noticeService.saveNoticeWithText(target.getUserId(), FcmPushType.BALANCE_RESULT, data, due.title(), due.body());
            fcmPushEventPublisher.publish(target.getUserId(), FcmPushType.BALANCE_RESULT, data, due.title(), due.body());
            sent++;
        }
        return sent;
    }

    // title·body: 알림함과 푸시가 같이 쓰는 문구. 저장해 두므로 나중에 판이 바뀌어도 그대로
    public record DueNotice(Long balanceGameId, Long contentId, String title, String body) {

        static DueNotice of(BalanceGame game) {
            // 타이틀 없이 등록된 판은 기본 문구
            String title = StringUtils.hasText(game.getTitle())
                    ? "투표 결과: " + game.getTitle()
                    : FcmPushType.BALANCE_RESULT.getTitle();
            return new DueNotice(game.getId(), game.getContent().getId(), title,
                    game.getOptionAText() + " vs " + game.getOptionBText());
        }
    }
}
