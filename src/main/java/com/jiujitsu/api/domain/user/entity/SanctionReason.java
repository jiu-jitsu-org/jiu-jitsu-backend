package com.jiujitsu.api.domain.user.entity;

public enum SanctionReason {
    PROFANITY,   // 욕설 및 비방
    ADVERTISING, // 광고 및 홍보
    OBSCENE,     // 음란 또는 부적절한 콘텐츠
    PROVOCATION, // 분쟁 유도
    HATE,        // 혐오 및 괴롭힘
    OTHER        // 기타 (reasonDetail 필수)
}
