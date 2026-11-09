package com.jiujitsu.api.domain.competition.service;

import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Base64;

/**
 * 대회 목록 커서 = 정렬 순서 (openDate, id) 위의 경계 + 방향.
 * AFTER(d, i) 는 (d, i) 보다 뒤, BEFORE(d, i) 는 (d, i) 보다 앞 항목을 가리킨다 (경계 자신은 포함하지 않음).
 * 필터는 담지 않는다 — 위치만 담으므로 필터가 바뀌어도 결과가 정의된다.
 * 바깥에는 Base64URL 로 감싼 불투명 문자열로만 내보낸다 (정렬 키를 바꿔도 계약 유지).
 */
public record CompetitionCursor(Direction direction, LocalDate openDate, long id) {

    public enum Direction { AFTER, BEFORE }

    private static final String SEPARATOR = "|";

    /**
     * 기준일 첫 페이지 = 그날(포함) 이후. id 는 1부터라 (date, 0) 뒤가 곧 openDate >= date
     */
    public static CompetitionCursor startOf(LocalDate date) {
        return new CompetitionCursor(Direction.AFTER, date, 0);
    }

    public static CompetitionCursor after(LocalDate openDate, long id) {
        return new CompetitionCursor(Direction.AFTER, openDate, id);
    }

    public static CompetitionCursor before(LocalDate openDate, long id) {
        return new CompetitionCursor(Direction.BEFORE, openDate, id);
    }

    public boolean isBefore() {
        return direction == Direction.BEFORE;
    }

    /**
     * 같은 경계에서 반대쪽 전부 (빈 페이지일 때 반대 방향 커서용).
     * AFTER(d, i) 의 반대 = (d, i) 이하 = BEFORE(d, i + 1), BEFORE(d, i) 의 반대 = (d, i) 이상 = AFTER(d, i - 1)
     */
    public CompetitionCursor opposite() {
        return isBefore() ? after(openDate, id - 1) : before(openDate, id + 1);
    }

    public String encode() {
        String raw = direction.name() + SEPARATOR + openDate + SEPARATOR + id;
        return Base64.getUrlEncoder().withoutPadding().encodeToString(raw.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 형식이 틀린 커서는 WRONG_PARAMETER
     */
    public static CompetitionCursor decode(String value) {
        try {
            String raw = new String(Base64.getUrlDecoder().decode(value), StandardCharsets.UTF_8);
            String[] parts = raw.split("\\" + SEPARATOR, -1);
            if (parts.length != 3) {
                throw new ErrorException(ErrorCode.WRONG_PARAMETER);
            }
            return new CompetitionCursor(Direction.valueOf(parts[0]), LocalDate.parse(parts[1]), Long.parseLong(parts[2]));
        } catch (IllegalArgumentException | DateTimeParseException e) {
            // Base64 오류·enum 이름 오류·숫자 오류(NumberFormatException)는 모두 IllegalArgumentException
            throw new ErrorException(ErrorCode.WRONG_PARAMETER);
        }
    }
}
