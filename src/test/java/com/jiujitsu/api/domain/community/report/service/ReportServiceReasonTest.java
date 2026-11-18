package com.jiujitsu.api.domain.community.report.service;

import com.jiujitsu.api.domain.community.board.entity.Board;
import com.jiujitsu.api.domain.community.board.repository.BoardRepository;
import com.jiujitsu.api.domain.community.comment.repository.CommunityCommentsRepository;
import com.jiujitsu.api.domain.community.report.dto.ReportCreateRequest;
import com.jiujitsu.api.domain.community.report.entity.Report;
import com.jiujitsu.api.domain.community.report.entity.ReportReason;
import com.jiujitsu.api.domain.community.report.entity.ReportType;
import com.jiujitsu.api.domain.community.report.mapper.ReportMapper;
import com.jiujitsu.api.domain.community.report.repository.ReportRepository;
import com.jiujitsu.api.domain.user.entity.User;
import com.jiujitsu.api.domain.user.entity.UserRole;
import com.jiujitsu.api.domain.user.repository.UserRepository;
import com.jiujitsu.api.domain.user.service.AuthenticationFacade;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Optional;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * 불법촬영물 신고도 다른 사유처럼 3건 누적일 때만 자동 숨김 (#244)
 */
@ExtendWith(MockitoExtension.class)
class ReportServiceReasonTest {

    private static final Long CONTENT_ID = 100L;

    @InjectMocks
    private ReportService reportService;

    @Mock private ReportRepository reportRepository;
    @Mock private BoardRepository boardRepository;
    @Mock private CommunityCommentsRepository communityCommentsRepository;
    @Mock private AuthenticationFacade authenticationFacade;
    @Mock private ReportMapper reportMapper;
    // 회원 신고(#233)가 ReportService 에 더하는 의존성. 지금 생성자에는 없어 무시되고, #233 머지 뒤에도 이 테스트가 그대로 돌게 미리 둔다
    @Mock private UserRepository userRepository;
    @Mock private TransactionTemplate transactionTemplate;

    @BeforeEach
    void runTransactionCallbackInline() {
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    @DisplayName("불법촬영물 신고 1건으로는 글을 숨기지 않고, 사유는 그대로 저장한다")
    void illegalFilming_singleReport_notHidden() {
        Board board = givenReportableBoard();
        given(reportRepository.countByReportTypeAndTargetId(ReportType.BOARD, CONTENT_ID)).willReturn(1L);

        reportService.createReport(request(ReportReason.ILLEGAL_FILMING));

        ArgumentCaptor<Report> saved = ArgumentCaptor.forClass(Report.class);
        verify(reportRepository).save(saved.capture());
        assertThat(saved.getValue().getReason()).isEqualTo(ReportReason.ILLEGAL_FILMING);
        verify(board, never()).hide();
    }

    @Test
    @DisplayName("불법촬영물 신고로 같은 글의 신고가 3건이 되면 숨긴다")
    void illegalFilming_thirdReport_hidden() {
        Board board = givenReportableBoard();
        given(reportRepository.countByReportTypeAndTargetId(ReportType.BOARD, CONTENT_ID)).willReturn(3L);

        reportService.createReport(request(ReportReason.ILLEGAL_FILMING));

        verify(board).hide();
    }

    private Board givenReportableBoard() {
        User reporter = user(1L);
        Board board = mock(Board.class);
        given(board.getCreatedBy()).willReturn(user(2L));
        lenient().when(board.isHidden()).thenReturn(false);
        given(authenticationFacade.getCurrentUser()).willReturn(reporter);
        given(reportRepository.existsByCreatedByAndReportTypeAndTargetId(reporter, ReportType.BOARD, CONTENT_ID)).willReturn(false);
        given(boardRepository.findByContent_Id(CONTENT_ID)).willReturn(Optional.of(board));
        given(reportRepository.save(any(Report.class))).willAnswer(invocation -> invocation.getArgument(0));
        return board;
    }

    private ReportCreateRequest request(ReportReason reason) {
        return new ReportCreateRequest(ReportType.BOARD, CONTENT_ID, reason, null);
    }

    private User user(Long id) {
        return User.builder().id(id).role(UserRole.USER).build();
    }
}
