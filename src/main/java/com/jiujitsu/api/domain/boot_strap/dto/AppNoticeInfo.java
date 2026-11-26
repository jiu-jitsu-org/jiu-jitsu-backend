package com.jiujitsu.api.domain.boot_strap.dto;

import com.jiujitsu.api.domain.boot_strap.entity.AppNotice;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "앱 시작 시 보여 줄 공지 (없으면 null)")
public record AppNoticeInfo(
        @Schema(description = "공지 ID") Long id,
        @Schema(description = "제목") String title,
        @Schema(description = "본문") String body,
        @Schema(description = "연결 링크 (없으면 null)") String link,
        @Schema(description = "앱 사용 차단 여부 (true 면 닫기 불가)") boolean blockApp
) {
    public static AppNoticeInfo from(AppNotice notice) {
        return new AppNoticeInfo(
                notice.getId(),
                notice.getTitle(),
                notice.getBody(),
                notice.getLink(),
                notice.isBlockApp()
        );
    }
}
