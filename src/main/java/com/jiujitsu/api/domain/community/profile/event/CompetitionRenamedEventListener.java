package com.jiujitsu.api.domain.community.profile.event;

import com.jiujitsu.api.domain.community.profile.repository.CompetitionRecordRepository;
import com.jiujitsu.api.domain.competition.event.CompetitionRenamedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 대회명·연도가 바뀌면 그 대회에 연결된 프로필 대회 기록도 맞춘다 (#208)
 * 발행 즉시 같은 트랜잭션에서 처리 (@EventListener) — 기록 갱신이 실패하면 대회 수정도 함께 되돌린다
 * 다른 리스너처럼 AFTER_COMMIT 으로 두면 커밋 뒤 실패 시 대회와 기록이 어긋난다
 */
@Component
@RequiredArgsConstructor
public class CompetitionRenamedEventListener {

    private final CompetitionRecordRepository competitionRecordRepository;

    @EventListener
    public void handle(CompetitionRenamedEvent event) {
        competitionRecordRepository.syncCompetitionName(event.competitionId(), event.name(), event.competitionYear());
    }
}
