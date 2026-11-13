package com.jiujitsu.api.domain.community.profile.repository;

import com.jiujitsu.api.domain.community.profile.entity.CompetitionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CompetitionRecordRepository extends JpaRepository<CompetitionRecord, Long> {

    // 등록 대회에서 고른 기록의 이름·연도를 대회 값으로 맞춘다 (관리자가 대회명·연도를 바꿈). 바뀐 행 수 반환
    // 프로필 저장도 연결 기록을 대회 값으로 덮어쓰므로(CompetitionDto#withRegistered) 같은 결과를 미리 반영하는 것
    // 영속성 컨텍스트는 비우지 않는다 — 호출한 관리자 요청은 기록 엔티티를 읽지 않고, 비우면 응답에 쓸 대회 엔티티가 분리돼 지연 로딩이 터진다
    @Modifying(flushAutomatically = true)
    @Query("UPDATE CompetitionRecord r SET r.competitionName = :name, r.competitionYear = :year WHERE r.competition.id = :competitionId")
    int syncCompetitionName(@Param("competitionId") Long competitionId, @Param("name") String name, @Param("year") Integer year);
}
