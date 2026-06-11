package com.jiujitsu.api.domain.community.report.repository;

import com.jiujitsu.api.domain.community.report.entity.Report;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {

    boolean existsByCreatedByAndReportTypeAndTargetId(User createdBy, ReportType reportType, Long targetId);

    long countByReportTypeAndTargetId(ReportType reportType, Long targetId);

    @Query("""
            SELECT r.targetId
            FROM Report r
            WHERE r.createdBy.id = :userId
              AND r.reportType = :reportType
            """)
    List<Long> findTargetIdsByCreatedByIdAndReportType(@Param("userId") Long userId,
                                                       @Param("reportType") ReportType reportType);

    @Query("""
            SELECT r FROM Report r
            LEFT JOIN FETCH r.createdBy
            ORDER BY r.createdAt DESC
            """)
    Page<Report> findAllWithReporter(Pageable pageable);

    @Query("""
            SELECT r.targetId, COUNT(r)
            FROM Report r
            WHERE r.reportType = :reportType
              AND r.targetId IN :targetIds
            GROUP BY r.targetId
            """)
    List<Object[]> countByReportTypeAndTargetIds(@Param("reportType") ReportType reportType,
                                                 @Param("targetIds") List<Long> targetIds);
}
