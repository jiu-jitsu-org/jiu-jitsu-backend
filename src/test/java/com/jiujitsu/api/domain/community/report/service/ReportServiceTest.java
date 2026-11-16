package com.jiujitsu.api.domain.community.report.service;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.entity.CommunityComments;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.content.entity.Content;
import com.jiujitsu.api.domain.community.report.dto.AdminReportListResponse;
import com.jiujitsu.api.domain.community.report.dto.ReportCreateRequest;
import com.jiujitsu.api.domain.community.report.entity.Report;
import com.jiujitsu.api.domain.community.report.entity.ReportReason;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.mapper.ReportMapper;
import com.jiujitsu.api.domain.community.report.repository.ReportCountProjection;
import com.jiujitsu.api.domain.community.report.repository.ReportRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.entity.UserStatus;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * 신고 — 회원 신고(USER)가 글·댓글 처리(대상 확인·자동 숨김·관리자 목록)로 새지 않는지 (#232)
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock private ReportRepository reportRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private CommunityCommentsRepository communityCommentsRepository;
    @Mock private UserRepository userRepository;
    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private TransactionTemplate transactionTemplate;
    @Spy private ReportMapper reportMapper = new ReportMapper();

    private static final Long ME = 1L;
    private static final Long TARGET = 2L;

    private final User me = user(ME, "나", UserStatus.ACTIVE);

    @Nested
    @DisplayName("회원 신고 등록")
    class CreateUserReport {

        @BeforeEach
        void setUp() {
            lenient().when(authenticationFacade.getCurrentUser()).thenReturn(me);
            willAnswer(invocation -> {
                invocation.<Consumer<Object>>getArgument(0).accept(null);
                return null;
            }).given(transactionTemplate).executeWithoutResult(any());
        }

        @Test
        @DisplayName("회원을 신고하면 USER·userId·사유로 저장한다")
        void create_saves() {
            given(userRepository.findById(TARGET)).willReturn(Optional.of(user(TARGET, "상대", UserStatus.ACTIVE)));

            reportService.createReport(request(TARGET));

            ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
            verify(reportRepository).save(saved.capture());
            assertThat(saved.getValue().getReportType()).isEqualTo(ReportType.USER);
            assertThat(saved.getValue().getTargetId()).isEqualTo(TARGET);
            assertThat(saved.getValue().getReason()).isEqualTo(ReportReason.ABUSE);
            assertThat(saved.getValue().getDetail()).isEqualTo("닉네임이 불쾌함");
        }

        @Test
        @DisplayName("회원 신고는 3건 이상 쌓여도 자동 처리하지 않고, 글·댓글을 조회하지 않는다")
        void create_noAutoHideAndNoContentLookup() {
            given(userRepository.findById(TARGET)).willReturn(Optional.of(user(TARGET, "상대", UserStatus.ACTIVE)));

            reportService.createReport(request(TARGET));

            verify(reportRepository, never()).countByReportTypeAndTargetId(any(), any());
            verifyNoInteractions(boardRepository, communityCommentsRepository);
        }

        @Test
        @DisplayName("내 계정은 신고할 수 없다 (U0007)")
        void create_self_rejected() {
            assertThatThrownBy(() -> reportService.createReport(request(ME)))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.SELF_REPORT_NOT_ALLOWED);
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("같은 회원을 다시 신고하면 U0008")
        void create_duplicate_rejected() {
            given(userRepository.findById(TARGET)).willReturn(Optional.of(user(TARGET, "상대", UserStatus.ACTIVE)));
            given(reportRepository.existsByCreatedByAndReportTypeAndTargetId(me, ReportType.USER, TARGET)).willReturn(true);

            assertThatThrownBy(() -> reportService.createReport(request(TARGET)))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_REPORTED);
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("동시 중복 신고로 유니크 제약에 걸리면 500 이 아니라 U0008")
        void create_uniqueViolation_alreadyReported() {
            given(userRepository.findById(TARGET)).willReturn(Optional.of(user(TARGET, "상대", UserStatus.ACTIVE)));
            given(reportRepository.save(any())).willThrow(new DataIntegrityViolationException("could not execute statement",
                    new SQLException("duplicate key value violates unique constraint", "23505")));

            assertThatThrownBy(() -> reportService.createReport(request(TARGET)))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.ALREADY_REPORTED);
        }

        @Test
        @DisplayName("유니크 위반이 아닌 무결성 오류(CHECK 제약 등)는 그대로 던진다")
        void create_otherIntegrityError_rethrows() {
            given(userRepository.findById(TARGET)).willReturn(Optional.of(user(TARGET, "상대", UserStatus.ACTIVE)));
            DataIntegrityViolationException failure = new DataIntegrityViolationException("could not execute statement",
                    new SQLException("violates check constraint", "23514"));
            given(reportRepository.save(any())).willThrow(failure);

            assertThatThrownBy(() -> reportService.createReport(request(TARGET))).isSameAs(failure);
        }

        @Test
        @DisplayName("없는 회원은 MEMBER_NOT_FOUND")
        void create_notFound() {
            given(userRepository.findById(TARGET)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.createReport(request(TARGET)))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("탈퇴(유예 포함) 회원은 MEMBER_NOT_FOUND")
        void create_deleted() {
            given(userRepository.findById(TARGET)).willReturn(Optional.of(user(TARGET, "상대", UserStatus.DELETED)));

            assertThatThrownBy(() -> reportService.createReport(request(TARGET)))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 신고한 회원이 그 뒤 탈퇴했으면 U0008 이 아니라 MEMBER_NOT_FOUND (차단과 같음)")
        void create_reportedThenDeleted_notFound() {
            lenient().when(reportRepository.existsByCreatedByAndReportTypeAndTargetId(me, ReportType.USER, TARGET)).thenReturn(true);
            given(userRepository.findById(TARGET)).willReturn(Optional.of(user(TARGET, "상대", UserStatus.DELETED)));

            assertThatThrownBy(() -> reportService.createReport(request(TARGET)))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
        }

        @Test
        @DisplayName("이미 신고한 게시물이 그 뒤 삭제됐으면 U0008 이 아니라 BOARD_NOT_FOUND (대상 확인이 중복 확인보다 먼저, 모든 타입 공통)")
        void create_boardReportedThenDeleted_notFound() {
            lenient().when(reportRepository.existsByCreatedByAndReportTypeAndTargetId(me, ReportType.BOARD, 10L)).thenReturn(true);
            given(boardRepository.findByContent_Id(10L)).willReturn(Optional.empty());

            assertThatThrownBy(() -> reportService.createReport(new ReportCreateRequest(ReportType.BOARD, 10L, ReportReason.SPAM, null)))
                    .isInstanceOf(ErrorException.class)
                    .extracting("errorCode")
                    .isEqualTo(ErrorCode.BOARD_NOT_FOUND);
            verify(reportRepository, never()).save(any());
        }

        @Test
        @DisplayName("게시물 신고는 그대로 3건째에 자동 숨김")
        void create_board_stillAutoHides() {
            Board board = mock(Board.class);
            given(board.getCreatedBy()).willReturn(user(TARGET, "상대", UserStatus.ACTIVE));
            given(boardRepository.findByContent_Id(10L)).willReturn(Optional.of(board));
            given(reportRepository.countByReportTypeAndTargetId(ReportType.BOARD, 10L)).willReturn(3L);

            reportService.createReport(new ReportCreateRequest(ReportType.BOARD, 10L, ReportReason.SPAM, null));

            verify(board).hide();
            verifyNoInteractions(userRepository);
        }

        private ReportCreateRequest request(Long targetId) {
            return new ReportCreateRequest(ReportType.USER, targetId, ReportReason.ABUSE, "닉네임이 불쾌함");
        }
    }

    @Nested
    @DisplayName("관리자 신고 목록")
    class AdminList {

        @Test
        @DisplayName("회원 신고는 대상 닉네임·그 회원 신고 수·숨김 false 로, 글·댓글 신고와 섞여도 각자 기준으로 나온다")
        void adminList_mixedTypes() {
            User reporter = user(ME, "신고자", UserStatus.ACTIVE);
            Report userReport = report(1L, ReportType.USER, TARGET, reporter);
            Report commentReport = report(2L, ReportType.COMMENT, TARGET, reporter);   // 같은 id 2 가 댓글 id 이기도 함
            Report boardReport = report(3L, ReportType.BOARD, 10L, reporter);
            PageRequest pageable = PageRequest.of(0, 20);
            given(reportRepository.findAllWithReporter(pageable))
                    .willReturn(new PageImpl<>(List.of(userReport, commentReport, boardReport), pageable, 3));

            given(userRepository.findAllById(List.of(TARGET))).willReturn(List.of(user(TARGET, "불쾌한닉네임", UserStatus.ACTIVE)));
            CommunityComments comment = mock(CommunityComments.class);
            given(comment.getId()).willReturn(TARGET);
            given(comment.getBody()).willReturn("댓글 본문");
            given(comment.isHidden()).willReturn(true);
            given(communityCommentsRepository.findAllById(List.of(TARGET))).willReturn(List.of(comment));
            Board board = mock(Board.class);
            Content content = mock(Content.class);
            given(content.getId()).willReturn(10L);
            given(board.getContent()).willReturn(content);
            given(board.getTitle()).willReturn("제목");
            given(board.getBody()).willReturn("본문");
            given(boardRepository.findAllByContentIdIn(List.of(10L))).willReturn(List.of(board));

            given(reportRepository.countByReportTypeAndTargetIds(ReportType.USER, List.of(TARGET))).willReturn(List.of(count(TARGET, 4)));
            given(reportRepository.countByReportTypeAndTargetIds(ReportType.COMMENT, List.of(TARGET))).willReturn(List.of(count(TARGET, 1)));
            given(reportRepository.countByReportTypeAndTargetIds(ReportType.BOARD, List.of(10L))).willReturn(List.of(count(10L, 2)));

            Page<AdminReportListResponse> page = reportService.getAdminReports(pageable);

            AdminReportListResponse userRow = page.getContent().get(0);
            assertThat(userRow.reportType()).isEqualTo(ReportType.USER);
            assertThat(userRow.targetId()).isEqualTo(TARGET);
            assertThat(userRow.targetPreview()).isEqualTo("불쾌한닉네임");
            assertThat(userRow.totalReportCount()).isEqualTo(4);
            assertThat(userRow.isHidden()).isFalse();

            AdminReportListResponse commentRow = page.getContent().get(1);
            assertThat(commentRow.targetPreview()).isEqualTo("댓글 본문");
            assertThat(commentRow.totalReportCount()).isEqualTo(1);
            assertThat(commentRow.isHidden()).isTrue();

            AdminReportListResponse boardRow = page.getContent().get(2);
            assertThat(boardRow.targetPreview()).isEqualTo("제목 본문");
            assertThat(boardRow.totalReportCount()).isEqualTo(2);
        }

        @Test
        @DisplayName("회원 신고가 없으면 회원 조회·카운트 쿼리를 보내지 않는다")
        void adminList_noUserReports_noUserQuery() {
            PageRequest pageable = PageRequest.of(0, 20);
            given(reportRepository.findAllWithReporter(pageable)).willReturn(new PageImpl<>(List.of(), pageable, 0));

            assertThat(reportService.getAdminReports(pageable).getContent()).isEmpty();

            verifyNoInteractions(userRepository);
            verify(reportRepository, never()).countByReportTypeAndTargetIds(eq(ReportType.USER), anyList());
        }
    }

    private static User user(Long id, String nickname, UserStatus status) {
        return User.builder().id(id).nickname(nickname).role(UserRole.USER).status(status).build();
    }

    private static Report report(Long id, ReportType type, Long targetId, User reporter) {
        Report report = mock(Report.class);
        given(report.getId()).willReturn(id);
        given(report.getReportType()).willReturn(type);
        given(report.getTargetId()).willReturn(targetId);
        given(report.getCreatedBy()).willReturn(reporter);
        given(report.getReason()).willReturn(ReportReason.ABUSE);
        lenient().when(report.getCreatedAt()).thenReturn(LocalDateTime.of(2026, 11, 16, 12, 0));
        return report;
    }

    private static ReportCountProjection count(Long targetId, long count) {
        return new ReportCountProjection() {
            @Override public Long getTargetId() { return targetId; }
            @Override public long getCount() { return count; }
        };
    }
}
