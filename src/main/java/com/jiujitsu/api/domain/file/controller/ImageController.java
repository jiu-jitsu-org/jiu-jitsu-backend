package com.jiujitsu.api.domain.file.controller;

import com.jiujitsu.api.domain.file.dto.CdnSignatureResponse;
import com.jiujitsu.api.domain.file.service.ImageService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExample;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "image-controller", description = "CDN 업로드 API")
@ApiErrorCodeExamples({ErrorCode.WRONG_PARAMETER, ErrorCode.AUTHENTICATION_FAILED})
@RestController
@RequestMapping("/api/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Operation(
            summary = "CDN 서버 서명 발급",
            description = "CDN 이미지 업로드 api 호출을 위한 서명을 발급합니다."
    )
    @ApiErrorCodeExample(ErrorCode.USER_NOT_FOUND)
    @GetMapping("/auth")
    public CdnSignatureResponse getCdnSignature() {
        return imageService.getCdnSignature();
    }
}
