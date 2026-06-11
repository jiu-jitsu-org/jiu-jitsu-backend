package com.jiujitsu.api.domain.community.report.repository;

public interface ReportCountProjection {
    Long getTargetId();
    long getCount();
}
