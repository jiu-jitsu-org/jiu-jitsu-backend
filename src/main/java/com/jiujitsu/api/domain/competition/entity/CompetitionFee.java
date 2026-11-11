package com.jiujitsu.api.domain.competition.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * 참가비 한 단계 (값 객체). 예: 얼리버드 60000원 ~11/8, 기본 참가비 70000원(기한 없음)
 * 단계 사이 규칙(순서·기한·얼리버드 마감과의 관계)은 Competition#validateEventInfo 가 본다
 */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class CompetitionFee {

    public static final int LABEL_MAX_LENGTH = 20;

    @Column(name = "label", nullable = false, length = LABEL_MAX_LENGTH)
    private String label;       // 단계 이름 (예: "얼리버드", "기본 참가비")

    @Column(name = "amount", nullable = false)
    private int amount;         // 금액 (원, 0 = 무료)

    @Column(name = "until")
    // 이 금액의 마지막 날(당일 포함). 할인 단계(얼리버드)에만 둔다 — 기한이 있으면 얼리버드로 본다
    // 기본 참가비는 접수 마감까지라도 null (접수 마감을 넣으면 얼리버드 마감일과 맞춰야 해 거절된다)
    private LocalDate until;
}
