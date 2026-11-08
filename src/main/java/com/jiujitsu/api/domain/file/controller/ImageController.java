package com.jiujitsu.api.domain.file.controller;

import com.jiujitsu.api.domain.file.dto.CdnSignatureResponse;
import com.jiujitsu.api.domain.file.dto.ImageFileRegisterRequest;
import com.jiujitsu.api.domain.file.dto.ImageFileResponse;
import com.jiujitsu.api.domain.file.service.ImageService;
import com.jiujitsu.api.global.exception.ErrorCode;
import com.jiujitsu.api.global.exception.annotation.ApiErrorCodeExamples;
import com.jiujitsu.api.global.exception.annotation.CommonApiResponses;
import com.jiujitsu.api.global.exception.annotation.LoginErrorExamples;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "image-controller", description = "CDN 업로드 API")
@CommonApiResponses
@RestController
@RequestMapping("/image")
@RequiredArgsConstructor
public class ImageController {

    private final ImageService imageService;

    @Operation(
            summary = "CDN 서버 서명 발급",
            description = "CDN 이미지 업로드 api 호출을 위한 서명을 발급합니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.FAILED_SIGNATURE})
    @GetMapping("/auth")
    public CdnSignatureResponse getCdnSignature() {
        return imageService.getCdnSignature();
    }

    @Operation(
            summary = "CDN 업로드 후 이미지 등록",
            description = "CDN에 업로드된 이미지의 cdnId와 imageUrl을 서버에 등록합니다. TEMP 상태로 저장되며, 게시물/프로필 저장 시 ACTIVE로 전환됩니다."
    )
    @LoginErrorExamples
    @PostMapping
    public ImageFileResponse registerImageFile(@RequestBody @Valid ImageFileRegisterRequest request) {
        return imageService.registerImageFile(request);
    }

    @Operation(
            summary = "이미지 삭제 (본인 소유 전용)",
            description = "본인이 업로드한 이미지 또는 본인 프로필 이미지를 삭제합니다. " +
                    "cdnId가 있는 경우 CDN에서도 함께 삭제되며, 프로필·인증서류 연결 참조도 정리됩니다."
    )
    @LoginErrorExamples
    @ApiErrorCodeExamples({ErrorCode.IMAGE_FILE_NOT_FOUND, ErrorCode.PERMISSION_DENIED, ErrorCode.FAILED_CDN_DELETE})
    @DeleteMapping("/{id}")
    public void deleteImageFile(
            @Parameter(description = "이미지 파일 ID") @PathVariable Long id
    ) {
        imageService.deleteMyImageFile(id);
    }
}
