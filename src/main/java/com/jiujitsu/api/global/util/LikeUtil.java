package com.jiujitsu.api.global.util;

public final class LikeUtil {

    private LikeUtil() {
    }

    // LIKE 특수문자(\ % _)를 이스케이프한다 (쿼리의 ESCAPE '\' 와 짝)
    public static String escape(String value) {
        return value
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
    }
}
