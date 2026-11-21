package com.jiujitsu.api.domain.announcement.controller;

import com.jiujitsu.api.domain.announcement.dto.AnnouncementDetailResponse;
import com.jiujitsu.api.domain.announcement.dto.AnnouncementListResponse;
import com.jiujitsu.api.domain.announcement.service.AnnouncementQueryService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "announcement-controller", description = "공지사항 API")
@CommonApiResponses
@RestController
@RequestMapping("/announcements")
@RequiredArgsConstructor
public class AnnouncementController {

    private final AnnouncementQueryService announcementQueryService;

    @Operation(
            summary = "공지 목록",
            description = "게시된 공지를 게시 시각 최신순(같으면 id 큰 순)으로 반환합니다. 비로그인도 조회할 수 있습니다.\n\n"
                    + "- 게시 시각이 지금 이전(같으면 포함)인 공지만. 게시 시각이 없거나 미래인 공지는 빠집니다\n"
                    + "- page · size 만 씁니다 (기본 size 20). sort 를 보내도 무시합니다\n"
                    + "- 공지가 없으면 빈 페이지\n"
                    + "- 알림(/notice)과 다릅니다"
    )
    @GetMapping
    public Page<AnnouncementListResponse> getList(@PageableDefault(size = 20) Pageable pageable) {
        return announcementQueryService.getList(pageable);
    }

    @Operation(
            summary = "공지 상세",
            description = "게시된 공지 하나를 반환합니다. 비로그인도 조회할 수 있습니다.\n\n"
                    + "- body 는 평문입니다. 줄바꿈(\\n)은 그대로이고, 링크는 화면에서 주소를 찾아 링크로 만듭니다\n"
                    + "- 없거나 아직 게시 전(게시 시각이 없거나 미래)이면 404 ANNOUNCEMENT_NOT_FOUND"
    )
    @ApiErrorCodeExamples({ErrorCode.ANNOUNCEMENT_NOT_FOUND})
    @GetMapping("/{announcementId}")
    public AnnouncementDetailResponse getDetail(
            @Parameter(description = "공지 ID", required = true) @PathVariable Long announcementId
    ) {
        return announcementQueryService.getDetail(announcementId);
    }
}
