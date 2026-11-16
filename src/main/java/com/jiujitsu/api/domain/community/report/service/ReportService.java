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
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import com.jiujitsu.api.global.util.ConstraintViolationUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private static final long AUTO_HIDE_THRESHOLD = 3L;

    private final ReportRepository reportRepository;
    private final BoardRepository boardRepository;
    private final CommunityCommentsRepository communityCommentsRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;
    private final AuthenticationFacade authenticationFacade;
    private final ReportMapper reportMapper;

    /**
     * 신고 등록 (게시물/댓글/회원)
     * targetId: BOARD → content.id, COMMENT → comment.id, USER → user.id
     * 같은 사람의 같은 대상 신고가 동시에 들어와 늦은 쪽이 (신고자, 종류, 대상) 유니크 제약에 걸리면 중복 신고(U0008)로 응답한다.
     * 에러가 난 트랜잭션은 다시 쓸 수 없으므로(PostgreSQL) TransactionTemplate 으로 실행한다 (좋아요와 같음, #199)
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void createReport(ReportCreateRequest request) {
        try {
            transactionTemplate.executeWithoutResult(status -> saveReport(request));
        } catch (DataIntegrityViolationException e) {
            // 이 트랜잭션이 쓰는 유니크는 (created_by, report_type, target_id)뿐이다 (PK 는 IDENTITY) → 유니크 위반이면 동시 중복 신고.
            // 그 밖의 위반(FK·CHECK 등)은 그대로 드러낸다
            if (!ConstraintViolationUtil.isUniqueViolation(e)) {
                throw e;
            }
            log.info("신고 동시 요청 충돌 → 중복 신고로 응답 reportType={} targetId={} cause={}",
                    request.reportType(), request.targetId(), e.getMostSpecificCause().getMessage());
            throw new ErrorException(ErrorCode.ALREADY_REPORTED);
        }
    }

    private void saveReport(ReportCreateRequest request) {
        User reporter = authenticationFacade.getCurrentUser();

        if (reportRepository.existsByCreatedByAndReportTypeAndTargetId(reporter, request.reportType(), request.targetId())) {
            throw new ErrorException(ErrorCode.ALREADY_REPORTED);
        }

        // 대상 검증 + 자기 신고 방지 (조회한 엔티티 재사용). 회원 신고는 숨길 대상이 없어 null
        Hideable target = validateTargetAndNotSelfReport(request, reporter);

        reportRepository.save(Report.builder()
                .reportType(request.reportType())
                .targetId(request.targetId())
                .reason(request.reason())
                .detail(request.detail())
                .build());

        // 회원 신고는 쌓여도 자동 처리하지 않는다 (관리자 목록에서만 본다, #232)
        if (target != null) {
            autoHideIfThresholdReached(request.reportType(), request.targetId(), target);
        }
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

        List<Long> boardTargetIds = distinctTargetIds(targetIdsByType, ReportType.BOARD);
        List<Long> commentTargetIds = distinctTargetIds(targetIdsByType, ReportType.COMMENT);
        List<Long> userTargetIds = distinctTargetIds(targetIdsByType, ReportType.USER);

        Map<Long, Board> boardMap = boardTargetIds.isEmpty()
                ? Map.of()
                : boardRepository.findAllByContentIdIn(boardTargetIds).stream()
                .collect(Collectors.toMap(b -> b.getContent().getId(), b -> b));

        Map<Long, CommunityComments> commentMap = commentTargetIds.isEmpty()
                ? Map.of()
                : communityCommentsRepository.findAllById(commentTargetIds).stream()
                .collect(Collectors.toMap(CommunityComments::getId, c -> c));

        Map<Long, User> userMap = userTargetIds.isEmpty()
                ? Map.of()
                : userRepository.findAllById(userTargetIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        Map<ReportType, Map<Long, Long>> reportCounts = Map.of(
                ReportType.BOARD, countReports(ReportType.BOARD, boardTargetIds),
                ReportType.COMMENT, countReports(ReportType.COMMENT, commentTargetIds),
                ReportType.USER, countReports(ReportType.USER, userTargetIds));

        return reports.map(report -> {
            long count = reportCounts.get(report.getReportType()).getOrDefault(report.getTargetId(), 0L);
            return switch (report.getReportType()) {
                case BOARD -> {
                    Board board = boardMap.get(report.getTargetId());
                    String preview = board != null ? board.getTitle() + " " + board.getBody() : "";
                    boolean hidden = board != null && board.isHidden();
                    yield reportMapper.toAdminListResponse(report, preview, count, hidden);
                }
                case COMMENT -> {
                    CommunityComments comment = commentMap.get(report.getTargetId());
                    String preview = comment != null ? comment.getBody() : "";
                    boolean hidden = comment != null && comment.isHidden();
                    yield reportMapper.toAdminListResponse(report, preview, count, hidden);
                }
                // 회원 신고: 미리보기는 지금 닉네임, 숨김 개념 없음 (자동 처리 없음)
                case USER -> {
                    User user = userMap.get(report.getTargetId());
                    String preview = user != null && user.getNickname() != null ? user.getNickname() : "";
                    yield reportMapper.toAdminListResponse(report, preview, count, false);
                }
            };
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

    // 대상 존재 확인 + 자기 신고 방지, 조회한 엔티티 반환 (회원 신고는 숨길 대상이 없어 null)
    private Hideable validateTargetAndNotSelfReport(ReportCreateRequest request, User reporter) {
        return switch (request.reportType()) {
            case BOARD -> {
                Board board = boardRepository.findByContent_Id(request.targetId())
                        .orElseThrow(() -> new ErrorException(ErrorCode.BOARD_NOT_FOUND));
                if (reporter.equals(board.getCreatedBy())) {
                    throw new ErrorException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
                }
                yield board;
            }
            case COMMENT -> {
                CommunityComments comment = communityCommentsRepository.findById(request.targetId())
                        .orElseThrow(() -> new ErrorException(ErrorCode.COMMENT_NOT_FOUND));
                if (reporter.equals(comment.getCreatedBy())) {
                    throw new ErrorException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
                }
                yield comment;
            }
            case USER -> {
                if (reporter.getId().equals(request.targetId())) {
                    throw new ErrorException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
                }
                // 탈퇴(유예 포함) 회원은 공개 프로필처럼 없는 회원 (404)
                userRepository.findById(request.targetId())
                        .filter(found -> found.getStatus() != UserStatus.DELETED)
                        .orElseThrow(() -> new ErrorException(ErrorCode.MEMBER_NOT_FOUND));
                yield null;
            }
        };
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

    private static List<Long> distinctTargetIds(Map<ReportType, List<Long>> targetIdsByType, ReportType reportType) {
        return targetIdsByType.getOrDefault(reportType, List.of()).stream().distinct().toList();
    }

    // 빈 목록이면 IN 절 쿼리를 보내지 않는다
    private Map<Long, Long> countReports(ReportType reportType, List<Long> targetIds) {
        return targetIds.isEmpty()
                ? Map.of()
                : toCountMap(reportRepository.countByReportTypeAndTargetIds(reportType, targetIds));
    }

    private Map<Long, Long> toCountMap(List<ReportCountProjection> projections) {
        return projections.stream()
                .collect(Collectors.toMap(ReportCountProjection::getTargetId, ReportCountProjection::getCount));
    }
}
