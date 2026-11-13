package com.jiujitsu.api.domain.competition.event;

/**
 * 관리자가 대회명·연도를 바꿈 (#208). 대회 도메인은 커뮤니티를 모르므로, 연결된 프로필 대회 기록은 커뮤니티 쪽 리스너가 맞춘다
 * 같은 트랜잭션에서 처리한다 (기록 갱신이 실패하면 대회 수정도 되돌린다 — 발행은 대회 저장 뒤)
 *
 * @param competitionId   바뀐 대회
 * @param name            바뀐 표시용 대회명
 * @param competitionYear 바뀐 연도
 */
public record CompetitionRenamedEvent(
        Long competitionId,
        String name,
        Integer competitionYear
) {
}
