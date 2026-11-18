package com.jiujitsu.api.domain.community.report.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiujitsu.api.domain.community.report.dto.ReportCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 신고 사유 '개인정보 노출 · 권리 침해' · '불법촬영물' 추가 (#244)
 */
class ReportReasonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("새 사유 두 개는 '기타' 앞에 있고, '기타'가 마지막이다")
    void newReasons_existBeforeOther() {
        assertThat(Arrays.asList(ReportReason.values())).containsExactly(
                ReportReason.SPAM,
                ReportReason.ABUSE,
                ReportReason.ADULT,
                ReportReason.HARASSMENT,
                ReportReason.INCITEMENT,
                ReportReason.RIGHTS_VIOLATION,
                ReportReason.ILLEGAL_FILMING,
                ReportReason.OTHER);
        assertThat(ReportReason.RIGHTS_VIOLATION.getDescription()).isEqualTo("개인정보 노출 · 권리 침해");
        assertThat(ReportReason.ILLEGAL_FILMING.getDescription()).isEqualTo("불법촬영물");
    }

    @ParameterizedTest
    @ValueSource(strings = {"RIGHTS_VIOLATION", "ILLEGAL_FILMING"})
    @DisplayName("신고 요청 본문의 reason 에 새 코드를 보내면 그 사유로 읽힌다")
    void createRequest_deserializesNewReasons(String code) throws Exception {
        String json = """
                {"reportType": "BOARD", "targetId": 1, "reason": "%s"}
                """.formatted(code);

        ReportCreateRequest request = objectMapper.readValue(json, ReportCreateRequest.class);

        assertThat(request.reason()).isEqualTo(ReportReason.valueOf(code));
    }
}
