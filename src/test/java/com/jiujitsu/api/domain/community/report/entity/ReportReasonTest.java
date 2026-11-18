package com.jiujitsu.api.domain.community.report.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jiujitsu.api.domain.community.report.dto.ReportCreateRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 신고 사유 '개인정보 노출 · 권리 침해' · '불법촬영물' 추가 (#244)
 */
class ReportReasonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
