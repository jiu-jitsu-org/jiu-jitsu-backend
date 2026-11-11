package com.jiujitsu.api.domain.notice.service;

import com.jiujitsu.api.domain.competition.entity.Competition;
import com.jiujitsu.api.domain.competition.repository.CompetitionRepository;
import com.jiujitsu.api.domain.notice.entity.CompetitionDeadlineNoticeLog;
import com.jiujitsu.api.domain.notice.repository.CompetitionDeadlineNoticeLogRepository;
import com.jiujitsu.api.domain.notice.repository.UserNoticeSettingRepository;
import com.jiujitsu.api.global.fcm.entity.FcmPushType;
import com.jiujitsu.api.global.fcm.entity.PushActionType;
import com.jiujitsu.api.global.fcm.event.FcmPushEventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 관심 대회 마감 전날 알림 (얼리버드·접수 마감). CompetitionDeadlineNoticeRunService 가 부른다 (매일 저녁·관리자 재실행)
 * 알림 하나 = 대회 하나의 마감 하나. 받는 사람: 관심 등록 + 이 대회 알림 켜짐 + 설정의 관심 대회 알림 켜짐
 */
@Service
@RequiredArgsConstructor
public class CompetitionDeadlineNoticeService {

    private final CompetitionRepository competitionRepository;
    private final UserNoticeSettingRepository userNoticeSettingRepository;
    private final CompetitionDeadlineNoticeLogRepository competitionDeadlineNoticeLogRepository;
    private final NoticeService noticeService;
    private final FcmPushEventPublisher fcmPushEventPublisher;

    /**
     * date 에 끝나는 마감 목록 (대회 정보는 이 트랜잭션 안에서 읽는다 — 참가비 단계가 지연 로딩)
     */
    @Transactional(readOnly = true)
    public List<DueNotice> findDue(LocalDate date) {
        return competitionRepository.findPublishedWithDeadlineOn(date).stream()
                .flatMap(competition -> CompetitionDeadline.dueOn(competition, date).stream()
                        .map(deadline -> new DueNotice(competition.getId(), competition.getName(), deadline)))
                .toList();
    }

    /**
     * 마감 하나를 받을 사람에게 보낸다. 이미 받은 유저는 건너뛴다 (발송 기록 = 멱등 키)
     * 마감 하나마다 트랜잭션을 따로 둬서 한 대회가 실패해도 나머지는 보낸다.
     * 서버 여러 대가 같은 마감을 동시에 보내면 기록의 유니크 제약에 걸린 쪽이 통째로 롤백된다 (알림·푸시 없음 — 푸시는 커밋 뒤 발송)
     * @return 보낸 유저 수
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public int send(DueNotice due) {
        List<Long> recipientIds = userNoticeSettingRepository.findCompetitionNoticeRecipientIds(due.competitionId());
        if (recipientIds.isEmpty()) {
            return 0;
        }
        CompetitionDeadline deadline = due.deadline();
        Set<Long> notified = competitionDeadlineNoticeLogRepository.findNotifiedUserIds(
                due.competitionId(), deadline.type(), deadline.date(), recipientIds);
        List<Long> targets = recipientIds.stream().filter(id -> !notified.contains(id)).toList();

        competitionDeadlineNoticeLogRepository.saveAllAndFlush(targets.stream()
                .map(userId -> new CompetitionDeadlineNoticeLog(due.competitionId(), deadline.type(), deadline.date(), userId))
                .toList());

        FcmPushType pushType = deadline.type().getPushType();
        Map<String, String> data = Map.of(
                "type", PushActionType.COMPETITION_DETAIL.name(),
                "data", String.valueOf(due.competitionId()));
        String title = deadline.title();
        String body = deadline.body(due.competitionName());
        for (Long userId : targets) {
            noticeService.saveNoticeWithText(userId, pushType, data, title, body);
            fcmPushEventPublisher.publish(userId, pushType, data, title, body);
        }
        return targets.size();
    }

    public record DueNotice(Long competitionId, String competitionName, CompetitionDeadline deadline) {
    }
}
