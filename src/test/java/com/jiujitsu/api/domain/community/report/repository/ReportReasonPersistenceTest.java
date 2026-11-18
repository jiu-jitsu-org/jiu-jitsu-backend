package com.jiujitsu.api.domain.community.report.repository;

import com.jiujitsu.api.domain.community.report.entity.Report;
import com.jiujitsu.api.domain.community.report.entity.ReportReason;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 새 신고 사유 저장과 자동 숨김 집계 (#244)
 * H2 기준이라 PostgreSQL CHECK 제약은 여기서 드러나지 않는다 — dev 배포 전 수동 DDL은 PR 본문 참고
 */
@DataJpaTest
class ReportReasonPersistenceTest {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private TestEntityManager em;

    private Report report(Long targetId, ReportReason reason) {
        return Report.builder()
                .reportType(ReportType.BOARD)
                .targetId(targetId)
                .reason(reason)
                .build();
    }

    @Test
    @DisplayName("새 사유로 신고를 저장하면 그 사유 그대로 읽힌다")
    void save_withNewReasons() {
        Long rightsId = reportRepository.save(report(1L, ReportReason.RIGHTS_VIOLATION)).getId();
        Long filmingId = reportRepository.save(report(2L, ReportReason.ILLEGAL_FILMING)).getId();
        em.flush();
        em.clear();

        assertThat(reportRepository.findById(rightsId).orElseThrow().getReason()).isEqualTo(ReportReason.RIGHTS_VIOLATION);
        assertThat(reportRepository.findById(filmingId).orElseThrow().getReason()).isEqualTo(ReportReason.ILLEGAL_FILMING);
    }

    @Test
    @DisplayName("자동 숨김 집계는 사유와 상관없이 같은 대상의 신고를 모두 센다")
    void count_mixedReasons_countsAll() {
        reportRepository.save(report(10L, ReportReason.SPAM));
        reportRepository.save(report(10L, ReportReason.RIGHTS_VIOLATION));
        reportRepository.save(report(10L, ReportReason.ILLEGAL_FILMING));
        reportRepository.save(report(11L, ReportReason.ILLEGAL_FILMING));
        em.flush();

        assertThat(reportRepository.countByReportTypeAndTargetId(ReportType.BOARD, 10L)).isEqualTo(3L);
    }
}
