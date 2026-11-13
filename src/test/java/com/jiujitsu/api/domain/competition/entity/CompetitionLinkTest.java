package com.jiujitsu.api.domain.competition.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class CompetitionLinkTest {

    private static final LocalDateTime FIRST = LocalDateTime.of(2026, 11, 10, 9, 0);
    private static final LocalDateTime LATER = LocalDateTime.of(2026, 11, 12, 9, 0);

    private final CompetitionLink link = new CompetitionLink(Competition.of("서울 오픈", 2026), CompetitionLinkType.BRACKET, "https://a.example.com");

    @Test
    @DisplayName("새 링크는 승인 전, 승인하면 처음 승인 시각을 남기고 숨겼다 다시 켜도 유지")
    void approveKeepsFirstTime() {
        assertThat(link.getStatus()).isEqualTo(CompetitionLinkStatus.PENDING);
        link.approve(FIRST);
        link.hide();
        assertThat(link.getStatus()).isEqualTo(CompetitionLinkStatus.HIDDEN);
        link.approve(LATER);

        assertThat(link.getStatus()).isEqualTo(CompetitionLinkStatus.APPROVED);
        assertThat(link.getApprovedAt()).isEqualTo(FIRST);
    }

    @Test
    @DisplayName("주소가 바뀌면 승인 전으로 돌아가고 승인 시각을 지움, 같은 주소면 그대로")
    void changeUrl() {
        link.approve(FIRST);

        link.changeUrl("https://a.example.com");
        assertThat(link.getStatus()).isEqualTo(CompetitionLinkStatus.APPROVED);

        link.changeUrl("https://b.example.com");
        assertThat(link.getStatus()).isEqualTo(CompetitionLinkStatus.PENDING);
        assertThat(link.getApprovedAt()).isNull();
        assertThat(link.getUrl()).isEqualTo("https://b.example.com");
    }
}
