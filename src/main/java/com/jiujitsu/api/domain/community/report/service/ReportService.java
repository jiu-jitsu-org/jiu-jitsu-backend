package com.jiujitsu.api.domain.community.report.service;

import com.jiujitsu.api.domain.community.Hideable;
import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.report.dto.AdminReportListResponse;
import com.jiujitsu.api.domain.community.report.dto.ReportCreateRequest;
import com.jiujitsu.api.domain.community.report.entity.Report;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.mapper.ReportMapper;
import com.jiujitsu.api.domain.community.report.repository.ReportCountProjection;
import com.jiujitsu.api.domain.community.report.repository.ReportRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private static final long AUTO_HIDE_THRESHOLD = 3L;

    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;
    private final CommunityCommentsRepository communityCommentsRepository;
    private final AuthenticationFacade authenticationFacade;
    private final ReportMapper reportMapper;

    /**
     * 신고 등록 (게시물/댓글)
     * targetId: BOARD → content.id, COMMENT → comment.id
     */
    public void createReport(ReportCreateRequest request) {
        User reporter = authenticationFacade.getCurrentUser();

        // 대상 검증 + 자기 신고 방지 (조회한 엔티티 재사용)
        if (reportRepository.existsByCreatedByAndReportTypeAndTargetId(reporter, request.reportType(), request.targetId())) {
            throw new ErrorException(ErrorCode.ALREADY_REPORTED);
        }

        Hideable target = validateTargetAndNotSelfReport(request, reporter);

        reportRepository.save(Report.builder()
                .reportType(request.reportType())
                .targetId(request.targetId())
                .reason(request.reason())
                .build());

        autoHideIfThresholdReached(request.reportType(), request.targetId(), target);
    }

    /**
     * 신고된 targetId 목록 반환 (현재 로그인 유저 기준)
     */
    @Transactional(readOnly = true)
    public List<Long> getReportedTargetIds(ReportType reportType) {
        return authenticationFacade.getCurrentUserOptional()
                .map(user -> reportRepository.findTargetIdsByCreatedByIdAndReportType(user.getId(), reportType))
                .orElse(Collections.emptyList());
    }

    /**
     * 특정 유저의 신고 여부 확인
     */
    @Transactional(readOnly = true)
    public boolean hasReported(User user, ReportType reportType, Long targetId) {
        return reportRepository.existsByCreatedByAndReportTypeAndTargetId(user, reportType, targetId);
    }

    /**
     * 관리자: 신고 목록 조회
     */
    @Transactional(readOnly = true)
    public Page<AdminReportListResponse> getAdminReports(Pageable pageable) {
        Page<Report> reports = reportRepository.findAllWithReporter(pageable);

        // 타입별 targetId 한 번에 분리
        Map<ReportType, List<Long>> targetIdsByType = reports.stream()
                .collect(Collectors.groupingBy(
                        Report::getReportType,
                        Collectors.mapping(Report::getTargetId, Collectors.toUnmodifiableList())
                ));

        List<Long> boardTargetIds = targetIdsByType.getOrDefault(ReportType.BOARD, List.of())
                .stream().distinct().toList();
        List<Long> commentTargetIds = targetIdsByType.getOrDefault(ReportType.COMMENT, List.of())
                .stream().distinct().toList();

        Map<Long, Board> boardMap = boardTargetIds.isEmpty()
                ? Map.of()
                : boardRepository.findAllByContentIdIn(boardTargetIds).stream()
                        .collect(Collectors.toMap(b -> b.getContent().getId(), b -> b));

        Map<Long, CommunityComments> commentMap = commentTargetIds.isEmpty()
                ? Map.of()
                : communityCommentsRepository.findAllById(commentTargetIds).stream()
                        .collect(Collectors.toMap(CommunityComments::getId, c -> c));

        Map<Long, Long> boardReportCounts = toCountMap(
                reportRepository.countByReportTypeAndTargetIds(ReportType.BOARD, boardTargetIds));
        Map<Long, Long> commentReportCounts = toCountMap(
                reportRepository.countByReportTypeAndTargetIds(ReportType.COMMENT, commentTargetIds));

        return reports.map(report -> {
            if (report.getReportType() == ReportType.BOARD) {
                Board board = boardMap.get(report.getTargetId());
                String preview = board != null ? board.getTitle() + " " + board.getBody() : "";
                boolean hidden = board != null && board.isHidden();
                long count = boardReportCounts.getOrDefault(report.getTargetId(), 0L);
                return reportMapper.toAdminListResponse(report, preview, count, hidden);
            } else {
                CommunityComments comment = commentMap.get(report.getTargetId());
                String preview = comment != null ? comment.getBody() : "";
                boolean hidden = comment != null && comment.isHidden();
                long count = commentReportCounts.getOrDefault(report.getTargetId(), 0L);
                return reportMapper.toAdminListResponse(report, preview, count, hidden);
            }
        });
    }

    /**
     * 관리자: 게시물 숨김 해제
     */
    public void unhideBoard(Long contentId) {
        Board board = boardRepository.findByContent_Id(contentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));
        board.unhide();
    }

    /**
     * 관리자: 댓글 숨김 해제
     */
    public void unhideComment(Long commentId) {
        CommunityComments comment = communityCommentsRepository.findById(commentId)
                .orElseThrow(() -> new ErrorException(ErrorCode.COMMENT_NOT_FOUND));
        comment.unhide();
    }

    // 대상 존재 확인 + 자기 신고 방지, 조회한 엔티티 반환
    private Hideable validateTargetAndNotSelfReport(ReportCreateRequest request, User reporter) {
        if (request.reportType() == ReportType.BOARD) {
            Board board = boardRepository.findByContent_Id(request.targetId())
                    .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));
            if (reporter.equals(board.getCreatedBy())) {
                throw new ErrorException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
            }
            return board;
        } else {
            CommunityComments comment = communityCommentsRepository.findById(request.targetId())
                    .orElseThrow(() -> new ErrorException(ErrorCode.COMMENT_NOT_FOUND));
            if (reporter.equals(comment.getCreatedBy())) {
                throw new ErrorException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
            }
            return comment;
        }
    }

    // 신고 수 임계값 도달 시 자동 숨김 (validate에서 조회한 엔티티 재사용)
    // 댓글에 대댓글이 있으면 isReported 플래그만 설정 (soft-delete 방식, 삭제와 구분)
    private void autoHideIfThresholdReached(ReportType reportType, Long targetId, Hideable target) {
        long count = reportRepository.countByReportTypeAndTargetId(reportType, targetId);
        if (count < AUTO_HIDE_THRESHOLD || target.isHidden()) return;

        if (reportType == ReportType.COMMENT) {
            CommunityComments comment = (CommunityComments) target;
            boolean isReply = !Objects.equals(comment.getParentId(), 0L);
            if (isReply || communityCommentsRepository.existsByParentId(targetId)) {
                comment.markAsReported();
            } else {
                comment.hide();
            }
        } else {
            target.hide();
        }
    }

    private Map<Long, Long> toCountMap(List<ReportCountProjection> projections) {
        return projections.stream()
                .collect(Collectors.toMap(ReportCountProjection::getTargetId, ReportCountProjection::getCount));
    }
}
