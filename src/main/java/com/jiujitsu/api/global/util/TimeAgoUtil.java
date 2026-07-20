package com.jiujitsu.api.global.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public final class TimeAgoUtil {

    private TimeAgoUtil() {}

    public static String format(LocalDateTime createdAt) {
        if (createdAt == null) {
            return "";
        }

        LocalDateTime now = LocalDateTime.now();

        Duration duration = Duration.between(createdAt, now);
        long minutes = duration.toMinutes();
        long hours = duration.toHours();

        if (createdAt.isAfter(now)) {
            return "방금 전";
        }

        if (minutes <= 0) {
            return "방금 전";
        }

        if (minutes < 60) {
            return minutes + "분 전";
        }

        if (hours < 24) {
            return hours + "시간 전";
        }

        long days = ChronoUnit.DAYS.between(createdAt, now);
        if (days < 31) {
            return days + "일 전";
        }

        long months = ChronoUnit.MONTHS.between(createdAt, now);
        if (months < 12) {
            return months + "달 전";
        }

        long years = ChronoUnit.YEARS.between(createdAt, now);
        return years + "년 전";
    }
}
