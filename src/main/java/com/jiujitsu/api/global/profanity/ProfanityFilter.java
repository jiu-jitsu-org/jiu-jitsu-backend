package com.jiujitsu.api.global.profanity;

import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.ErrorException;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 금칙어 필터
 * 글·댓글 등록 전 호출해 금칙어 포함 시 C0016 으로 거부합니다.
 *
 * <p>목록은 정책 확인 후 보완합니다.
 * 주짓수 기술 이름(초크·암바·넥 크랭크 등)과 주짓수 은어가 걸리지 않게 좁게 유지합니다.
 */
@Component
public class ProfanityFilter {

    // 금칙어 목록 — 정책 확인 후 보완 예정
    private static final List<String> BANNED_WORDS = List.of(
            // 한국어
            "씨발", "씨팔", "시발", "시팔", "ㅅㅂ",
            "개새끼", "새끼",
            "병신", "ㅂㅅ",
            "보지", "자지",
            "창녀", "창년",
            "지랄",
            "존나", "졸라",
            // 영어
            "fuck", "shit", "asshole", "bitch", "cunt"
    );

    /**
     * 전달된 텍스트 중 금칙어가 포함된 것이 있으면 {@link ErrorCode#BANNED_WORD_DETECTED} 예외를 던집니다.
     * null 은 건너뜁니다.
     */
    public void validate(String... texts) {
        for (String text : texts) {
            if (text == null || text.isBlank()) continue;
            String lower = text.toLowerCase();
            for (String word : BANNED_WORDS) {
                if (lower.contains(word)) {
                    throw new ErrorException(ErrorCode.BANNED_WORD_DETECTED);
                }
            }
        }
    }
}
